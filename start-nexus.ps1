[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$nexusRoot = $PSScriptRoot
$ragRoot = Join-Path $nexusRoot "rag-service"
$backendRoot = Join-Path $nexusRoot "ai-backend\sovereign-ai-workbench"
$frontendRoot = Join-Path $nexusRoot "frontend-vite"
$runtimeRoot = Join-Path $backendRoot "storage\runtime"
$statePath = Join-Path $runtimeRoot "nexus-processes.json"
$startedProcesses = @()

New-Item -ItemType Directory -Force -Path $runtimeRoot | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $ragRoot ".runtime\chroma") | Out-Null

# Preserve valid process records when this command is run while Nexus is already online.
# Without this, a second start used to replace the state file with an empty array,
# leaving stop-nexus.ps1 unable to stop the services it originally launched.
if (Test-Path -LiteralPath $statePath) {
    $savedProcesses = Get-Content -LiteralPath $statePath -Raw | ConvertFrom-Json
    foreach ($record in $savedProcesses) {
        try {
            $process = Get-Process -Id ([int]$record.id) -ErrorAction SilentlyContinue
            if ($null -eq $process) { continue }

            $actualStart = $process.StartTime.ToUniversalTime()
            $savedStart = ([DateTimeOffset]$record.startedAtUtc).UtcDateTime
            if ($process.ProcessName -eq $record.processName -and [Math]::Abs(($actualStart - $savedStart).TotalMilliseconds) -lt 1) {
                $startedProcesses += $record
            }
        }
        catch {
            Write-Warning "Skipped an invalid Nexus process record: $($_.Exception.Message)"
        }
    }
}

function Test-NexusEndpoint {
    param([Parameter(Mandatory)][string]$Url)

    try {
        Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 3 | Out-Null
        return $true
    }
    catch {
        return $false
    }
}

function Save-NexusState {
    ConvertTo-Json -InputObject @($script:startedProcesses) -Depth 4 |
        Set-Content -LiteralPath $statePath -Encoding UTF8
}

function Add-NexusListeningProcess {
    param(
        [Parameter(Mandatory)][string]$Label,
        [Parameter(Mandatory)][int]$Port
    )

    try {
        $connection = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction Stop |
            Select-Object -First 1
        if ($null -eq $connection) { return }

        $process = Get-Process -Id ([int]$connection.OwningProcess) -ErrorAction Stop
        if ($startedProcesses | Where-Object { $_.id -eq $process.Id -or $_.label -eq $Label }) { return }

        $script:startedProcesses += [pscustomobject]@{
            label = $Label
            id = $process.Id
            processName = $process.ProcessName
            startedAtUtc = $process.StartTime.ToUniversalTime().ToString("o")
        }
    }
    catch {
        Write-Warning "Could not track the existing $Label process on port ${Port}: $($_.Exception.Message)"
    }
}

function Start-NexusProcess {
    param(
        [Parameter(Mandatory)][string]$Label,
        [Parameter(Mandatory)][string]$FilePath,
        [Parameter(Mandatory)][string[]]$ArgumentList,
        [Parameter(Mandatory)][string]$WorkingDirectory
    )

    $stdoutPath = Join-Path $runtimeRoot "$Label.out.log"
    $stderrPath = Join-Path $runtimeRoot "$Label.err.log"
    $process = Start-Process `
        -FilePath $FilePath `
        -ArgumentList $ArgumentList `
        -WorkingDirectory $WorkingDirectory `
        -WindowStyle Hidden `
        -RedirectStandardOutput $stdoutPath `
        -RedirectStandardError $stderrPath `
        -PassThru

    $script:startedProcesses += [pscustomobject]@{
        label = $Label
        id = $process.Id
        processName = $process.ProcessName
        startedAtUtc = $process.StartTime.ToUniversalTime().ToString("o")
    }
    Save-NexusState
    Write-Host "Started $Label (PID $($process.Id))."
}

function Wait-NexusEndpoint {
    param(
        [Parameter(Mandatory)][string]$Label,
        [Parameter(Mandatory)][string]$Url,
        [int]$TimeoutSeconds = 180
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if (Test-NexusEndpoint -Url $Url) {
            Write-Host "$Label is ready: $Url"
            return
        }
        Start-Sleep -Seconds 2
    }

    throw "$Label did not become ready. Check logs in $runtimeRoot"
}

if (-not (Test-NexusEndpoint -Url "http://127.0.0.1:11434/api/tags")) {
    $ollamaCommand = (Get-Command "ollama.exe" -ErrorAction Stop).Source
    Start-NexusProcess -Label "ollama" -FilePath $ollamaCommand -ArgumentList @("serve") -WorkingDirectory $nexusRoot
    Wait-NexusEndpoint -Label "Ollama" -Url "http://127.0.0.1:11434/api/tags" -TimeoutSeconds 60
}
else {
    Write-Host "Ollama is already running."
    Add-NexusListeningProcess -Label "ollama" -Port 11434
}

$warmModel = "llama3.1:8b"
$localPropertiesPath = Join-Path $backendRoot "local.properties"
if (Test-Path -LiteralPath $localPropertiesPath) {
    $configuredModelLine = Get-Content -LiteralPath $localPropertiesPath |
        Where-Object { $_ -match '^ai\.general\.model=' } |
        Select-Object -First 1
    if ($configuredModelLine) {
        $warmModel = ($configuredModelLine -split '=', 2)[1].Trim()
    }
}
try {
    Write-Host "Warming local model $warmModel for faster first response..."
    $warmBody = @{
        model = $warmModel
        prompt = ""
        stream = $false
        keep_alive = "10m"
        options = @{ num_predict = 1 }
    } | ConvertTo-Json -Depth 4
    Invoke-RestMethod -Uri "http://127.0.0.1:11434/api/generate" -Method Post -ContentType "application/json" -Body $warmBody -TimeoutSec 180 | Out-Null
    Write-Host "Local model is warm."
}
catch {
    Write-Warning "Model warm-up was skipped: $($_.Exception.Message)"
}

if (-not (Test-NexusEndpoint -Url "http://127.0.0.1:8001/health")) {
    $ragPython = Join-Path $ragRoot "venv\Scripts\python.exe"
    if (-not (Test-Path -LiteralPath $ragPython)) {
        throw "RAG virtual environment is missing. Run setup-nexus.ps1 first."
    }
    Start-NexusProcess `
        -Label "rag" `
        -FilePath $ragPython `
        -ArgumentList @("-m", "uvicorn", "main:app", "--host", "127.0.0.1", "--port", "8001") `
        -WorkingDirectory $ragRoot
    Wait-NexusEndpoint -Label "RAG service" -Url "http://127.0.0.1:8001/health"
}
else {
    Write-Host "RAG service is already running."
    Add-NexusListeningProcess -Label "rag" -Port 8001
}

if (-not (Test-NexusEndpoint -Url "http://127.0.0.1:8090/ai/health")) {
    $javaCommand = (Get-Command "java.exe" -ErrorAction Stop).Source
    $backendJar = Join-Path $backendRoot "target\sovereign-ai-workbench-0.0.1-SNAPSHOT.jar"
    if (-not (Test-Path -LiteralPath $backendJar)) {
        throw "Backend JAR is missing. Run setup-nexus.ps1 first."
    }
    Start-NexusProcess `
        -Label "backend" `
        -FilePath $javaCommand `
        -ArgumentList @("-jar", "target/sovereign-ai-workbench-0.0.1-SNAPSHOT.jar") `
        -WorkingDirectory $backendRoot
    Wait-NexusEndpoint -Label "Agent Engine" -Url "http://127.0.0.1:8090/ai/health"
}
else {
    Write-Host "Agent Engine is already running."
    Add-NexusListeningProcess -Label "backend" -Port 8090
}

if (-not (Test-NexusEndpoint -Url "http://127.0.0.1:3000")) {
    $nodeCommand = (Get-Command "node.exe" -ErrorAction Stop).Source
    $viteCli = Join-Path $frontendRoot "node_modules\vite\bin\vite.js"
    if (-not (Test-Path -LiteralPath $viteCli)) {
        throw "Vite is missing. Run setup-nexus.ps1 first."
    }
    Start-NexusProcess `
        -Label "frontend" `
        -FilePath $nodeCommand `
        -ArgumentList @("node_modules/vite/bin/vite.js", "--host", "127.0.0.1", "--port", "3000") `
        -WorkingDirectory $frontendRoot
    Wait-NexusEndpoint -Label "Frontend" -Url "http://127.0.0.1:3000" -TimeoutSeconds 60
}
else {
    Write-Host "Frontend is already running."
    Add-NexusListeningProcess -Label "frontend" -Port 3000
}

Save-NexusState
Write-Host ""
Write-Host "Nexus is ready: http://127.0.0.1:3000"
Write-Host "Logs: $runtimeRoot"
Write-Host "Stop it with: powershell -ExecutionPolicy Bypass -File .\stop-nexus.ps1"
