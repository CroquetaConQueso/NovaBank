$scriptDir = Split-Path -Parent $PSCommandPath
$projectRoot = Split-Path -Parent $scriptDir
$envFile = Join-Path $projectRoot ".env"

if ($MyInvocation.InvocationName -ne ".") {
    Write-Warning "Ejecuta este script con dot-sourcing para conservar las variables en la terminal actual: . .\scripts\load-env.ps1"
}

if (-not (Test-Path -LiteralPath $envFile)) {
    Write-Error "No se encontro .env en la raiz del proyecto. Copia .env.example a .env y rellena tus valores locales."
    return
}

$loaded = New-Object System.Collections.Generic.List[string]

foreach ($line in Get-Content -LiteralPath $envFile) {
    $trimmed = $line.Trim()

    if ([string]::IsNullOrWhiteSpace($trimmed) -or $trimmed.StartsWith("#")) {
        continue
    }

    if ($trimmed -notmatch "^\s*([A-Za-z_][A-Za-z0-9_]*)=(.*)$") {
        Write-Warning "Linea ignorada porque no tiene formato CLAVE=VALOR: $line"
        continue
    }

    $name = $Matches[1]
    $value = $Matches[2].Trim()

    if (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'"))) {
        $value = $value.Substring(1, $value.Length - 2)
    }

    [Environment]::SetEnvironmentVariable($name, $value, "Process")
    $loaded.Add($name)
}

Write-Host "Variables cargadas desde .env:"
foreach ($name in $loaded) {
    $value = [Environment]::GetEnvironmentVariable($name, "Process")
    if ($name -match "(PASSWORD|SECRET|TOKEN|KEY)") {
        Write-Host " - $name=***"
    } else {
        Write-Host " - $name=$value"
    }
}

Write-Host "Total: $($loaded.Count) variables."
