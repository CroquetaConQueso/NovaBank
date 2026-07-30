param(
    [string]$DockerHubUser = $env:DOCKERHUB_USER,
    [string]$Version = $env:VERSION,
    [switch]$Latest
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

foreach ($service in $services) {
    $localImage = "novabank/${service}:local"
    $versionedImage = "${DockerHubUser}/novabank-${service}:${Version}"

    docker image inspect $localImage *> $null
    if ($LASTEXITCODE -ne 0) {
        throw "No existe la imagen local ${localImage}. Ejecuta primero: docker compose build"
    }

    Write-Host "Etiquetando ${localImage} -> ${versionedImage}"
    docker tag $localImage $versionedImage

    if ($Latest) {
        $latestImage = "${DockerHubUser}/novabank-${service}:latest"
        Write-Host "Etiquetando ${localImage} -> ${latestImage}"
        docker tag $localImage $latestImage
    }
}

Write-Host "Etiquetado completado. Revisa las imagenes antes de publicar:"
docker images "${DockerHubUser}/novabank-*"
