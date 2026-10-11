# One-click launcher for MinIO (standalone Windows exe).
# Purpose: start object storage for the lost-and-found project WITHOUT Docker.
# Effect matches docker-compose minio service: API on :9000, console on :9010.
# Features: pre-check port 9000 to avoid duplicate startup.
# NOTE: keep this file ASCII-only to avoid PowerShell 5.1 encoding issues (no BOM).

$ErrorActionPreference = 'Stop'

# Executable now lives inside the project (portable): <project>/minio/minio.exe
$ExePath  = Join-Path $PSScriptRoot 'minio\minio.exe'
# Data directory is also inside the project now (fully self-contained, no dependency on E drive).
$DataDir  = Join-Path $PSScriptRoot 'minio\data'

# Credentials must match application.yml (minio.access-key / secret-key).
$env:MINIO_ROOT_USER     = 'minioadmin'
$env:MINIO_ROOT_PASSWORD = 'minioadmin'

# ---- 1. Port check: if 9000 is already listening, MinIO is likely running ----
$running = Get-NetTCPConnection -State Listen -LocalPort 9000 -ErrorAction SilentlyContinue
if ($running) {
    Write-Host '[INFO] Port 9000 already in use, MinIO seems to be running. Skip startup.' -ForegroundColor Yellow
    Write-Host '       Console: http://localhost:9010  (login: minioadmin / minioadmin)'
    exit 0
}

# ---- 2. Executable check ----
if (-not (Test-Path -LiteralPath $ExePath)) {
    Write-Host "[FAIL] minio.exe not found at: $ExePath" -ForegroundColor Red
    Write-Host '       Expected inside the project folder: <project>\minio\minio.exe'
    exit 1
}

# ---- 3. Data directory preparation ----
if (-not (Test-Path -LiteralPath $DataDir)) {
    New-Item -ItemType Directory -Force -Path $DataDir | Out-Null
    Write-Host "[INIT] Created data directory: $DataDir"
}

Write-Host '[START] Starting MinIO ...' -ForegroundColor Green
Write-Host '        API      : http://localhost:9000'
Write-Host '        Console  : http://localhost:9010  (login: minioadmin / minioadmin)'
Write-Host '        Keep this window open, press Ctrl+C to stop.'
Write-Host ''

& $ExePath server $DataDir --console-address ":9010"
