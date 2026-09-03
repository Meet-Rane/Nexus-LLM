[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$nexusRoot = $PSScriptRoot
$runtimeRoot = Join-Path $nexusRoot "ai-backend\sovereign-ai-workbench\storage\runtime"
$statePath = Join-Path $runtimeRoot "nexus-processes.json"

if (-not (Test-Path -LiteralPath $statePath)) {
    Write-Host "No Nexus process state was found. The services may already be stopped."
    exit 0
}

$records = Get-Content -LiteralPath $statePath -Raw | ConvertFrom-Json
for ($recordIndex = $records.Count - 1; $recordIndex -ge 0; $recordIndex--) {
    $record = $records[$recordIndex]
    $process = Get-Process -Id ([int]$record.id) -ErrorAction SilentlyContinue
    if ($null -eq $process) {
        continue
    }

    $actualStart = $process.StartTime.ToUniversalTime().ToString("o")
    if ($process.ProcessName -ne $record.processName -or $actualStart -ne $record.startedAtUtc) {
        Write-Warning "Skipped PID $($record.id): it no longer matches the recorded $($record.label) process."
        continue
    }

    & taskkill.exe /PID $process.Id /T /F | Out-Null
    Write-Host "Stopped $($record.label) (PID $($record.id))."
}

Remove-Item -LiteralPath $statePath -Force
Write-Host "Nexus services started by start-nexus.ps1 have been stopped."
