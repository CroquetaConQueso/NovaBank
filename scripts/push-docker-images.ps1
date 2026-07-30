param(
    [string]$DockerHubUser = $env:DOCKERHUB_USER,
    [string]$Version = $env:VERSION,
    [switch]$Latest,
    [switch]$Yes
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($DockerHubUser)) {
    throw "Define -DockerHubUser o la variable de entorno DOCKERHUB_USER."
}

if ([string]::IsNullOrWhiteSpace($Version)) {
    throw "Define -Version o la variable de entorno VERSION."
}

$services = @(
    "eureka-server",
    "config-server",
    "api-gateway",
    "auth-server",
    "cliente-service",
    "cuenta-service",
    "operacion-service",
    "exchange-rate-mock-service",
    "notificacion-service",
    "documento-service"
)

$images = foreach ($service in $services) {
    "${DockerHubUser}/novabank-${service}:${Version}"
    if ($Latest) {
        "${DockerHubUser}/novabank-${service}:latest"
    }
}

Write-Host "Imagenes que se publicarian en Docker Hub:"
$images | ForEach-Object { Write-Host " - $_" }

if (-not $Yes) {
    $confirmation = Read-Host "Escribe PUSH para continuar. Cualquier otro valor cancela"
    if ($confirmation -ne "PUSH") {
        Write-Host "Publicacion cancelada. No se ha ejecutado docker push."
        exit 0
    }
}

foreach ($image in $images) {
    docker image inspect $image *> $null
    if ($LASTEXITCODE -ne 0) {
        throw "No existe la imagen etiquetada ${image}. Ejecuta primero scripts/tag-docker-images.ps1."
    }

    Write-Host "Publicando ${image}"
    docker push $image
}

Write-Host "Publicacion completada."
