[CmdletBinding()]
param(
    [string]$Model = "llama3.1:8b",
    [string]$PythonExecutable = "C:\Program Files\Blender Foundation\Blender 5.0\5.0\python\bin\python.exe"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$nexusRoot = $PSScriptRoot
$ragRoot = Join-Path $nexusRoot "rag-service"
$frontendRoot = Join-Path $nexusRoot "frontend-vite"
$backendRoot = Join-Path $nexusRoot "ai-backend\sovereign-ai-workbench"

foreach ($command in @("node.exe", "npm.cmd", "java.exe", "mvn.cmd", "ollama.exe")) {
    Get-Command $command -ErrorAction Stop | Out-Null
}

if (-not (Test-Path -LiteralPath $PythonExecutable)) {
    throw "Python was not found at '$PythonExecutable'. Pass its full path with -PythonExecutable."
}

Write-Host "Writing local-only service configuration..."
@"
VITE_AGENT_URL=http://127.0.0.1:8090
VITE_RAG_URL=http://127.0.0.1:8001
"@ | Set-Content -LiteralPath (Join-Path $frontendRoot ".env.local") -Encoding ASCII

@"
HF_HUB_OFFLINE=1
TRANSFORMERS_OFFLINE=1
CHROMA_PERSIST_DIR=./.runtime/chroma
"@ | Set-Content -LiteralPath (Join-Path $ragRoot ".env") -Encoding ASCII

@"
ai.provider=ollama
sovereign.enforce-local-only=true
spring.ai.ollama.base-url=http://127.0.0.1:11434
spring.ai.ollama.chat.model=$Model
ai.coding.model=$Model
ai.general.model=$Model
rag.service.base-url=http://127.0.0.1:8001
sandbox.network-disabled=true
"@ | Set-Content -LiteralPath (Join-Path $backendRoot "local.properties") -Encoding ASCII

Write-Host "Installing frontend dependencies..."
Push-Location $frontendRoot
try {
    & npm.cmd install
    if ($LASTEXITCODE -ne 0) { throw "npm install failed." }
}
finally {
    Pop-Location
}

$ragPython = Join-Path $ragRoot "venv\Scripts\python.exe"
if (-not (Test-Path -LiteralPath $ragPython)) {
    Write-Host "Creating the RAG virtual environment..."
    & $PythonExecutable -m venv (Join-Path $ragRoot "venv")
    if ($LASTEXITCODE -ne 0) { throw "Python virtual-environment creation failed." }
}

Write-Host "Installing RAG dependencies..."
& $ragPython -m pip install --upgrade pip
if ($LASTEXITCODE -ne 0) { throw "pip upgrade failed." }
& $ragPython -m pip install -r (Join-Path $ragRoot "requirements.txt")
if ($LASTEXITCODE -ne 0) { throw "RAG dependency installation failed." }

Write-Host "Caching the local embedding model..."
& $ragPython -c "from sentence_transformers import SentenceTransformer; SentenceTransformer('all-MiniLM-L6-v2'); print('Embedding model ready.')"
if ($LASTEXITCODE -ne 0) { throw "Embedding model setup failed." }

$installedModels = (& ollama.exe list 2>$null) -join "`n"
if ($installedModels -notmatch [regex]::Escape($Model)) {
    Write-Host "Downloading Ollama model $Model..."
    & ollama.exe pull $Model
    if ($LASTEXITCODE -ne 0) { throw "Ollama model installation failed." }
}
else {
    Write-Host "Ollama model $Model is already installed."
}

if (Get-Command "docker.exe" -ErrorAction SilentlyContinue) {
    Write-Host "Building the secure Python sandbox..."
    & docker.exe build -t sovereign-python-sandbox (Join-Path $backendRoot "sandbox")
    if ($LASTEXITCODE -ne 0) { throw "Docker sandbox build failed." }
}
else {
    Write-Warning "Docker is not installed. Code execution will remain unavailable."
}

Write-Host "Running frontend checks..."
Push-Location $frontendRoot
try {
    & npm.cmd run lint
    if ($LASTEXITCODE -ne 0) { throw "Frontend lint failed." }
    & npm.cmd run build
    if ($LASTEXITCODE -ne 0) { throw "Frontend build failed." }
}
finally {
    Pop-Location
}

Write-Host "Running backend tests..."
Push-Location $backendRoot
try {
    & mvn.cmd test
    if ($LASTEXITCODE -ne 0) { throw "Backend tests failed." }
}
finally {
    Pop-Location
}

Write-Host ""
Write-Host "Permanent Nexus setup completed."
Write-Host "Start it with: powershell -ExecutionPolicy Bypass -File .\start-nexus.ps1"
