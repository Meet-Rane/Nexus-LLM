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
}

if (-not (Test-NexusEndpoint -Url "http://127.0.0.1:8090/ai/health")) {
    $mavenCommand = (Get-Command "mvn.cmd" -ErrorAction Stop).Source
    Start-NexusProcess `
        -Label "backend" `
        -FilePath $mavenCommand `
        -ArgumentList @("spring-boot:run") `
        -WorkingDirectory $backendRoot
    Wait-NexusEndpoint -Label "Agent Engine" -Url "http://127.0.0.1:8090/ai/health"
}
else {
    Write-Host "Agent Engine is already running."
}

if (-not (Test-NexusEndpoint -Url "http://127.0.0.1:3000")) {
    $npmCommand = (Get-Command "npm.cmd" -ErrorAction Stop).Source
    Start-NexusProcess `
        -Label "frontend" `
        -FilePath $npmCommand `
        -ArgumentList @("run", "dev", "--", "--host", "127.0.0.1", "--port", "3000") `
        -WorkingDirectory $frontendRoot
    Wait-NexusEndpoint -Label "Frontend" -Url "http://127.0.0.1:3000" -TimeoutSeconds 60
}
else {
    Write-Host "Frontend is already running."
}

Save-NexusState
Write-Host ""
Write-Host "Nexus is ready: http://127.0.0.1:3000"
Write-Host "Logs: $runtimeRoot"
Write-Host "Stop it with: powershell -ExecutionPolicy Bypass -File .\stop-nexus.ps1"
