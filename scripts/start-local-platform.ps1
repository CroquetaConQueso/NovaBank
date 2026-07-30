param(
    [switch]$Build
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot

Push-Location $repoRoot
try {
    if ($Build) {
        docker compose build
    }

    docker compose up -d
    docker compose ps
} finally {
    Pop-Location
}
