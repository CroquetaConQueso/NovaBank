param(
    [string]$FunctionName = "novabank-comision",
    [string]$EndpointUrl = "http://localhost:4566",
    [string]$Region = "eu-west-1"
)

$ErrorActionPreference = "Stop"

if (-not (Get-Command aws -ErrorAction SilentlyContinue)) {
    throw "AWS CLI no está instalado o no está en el PATH."
}

if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    throw "Maven no está instalado o no está en el PATH."
}

$repoRoot = Split-Path -Parent $PSScriptRoot
$jarPath = Join-Path $repoRoot "comision-lambda\target\comision-lambda-4.0-SNAPSHOT-aws.jar"
$handler = "com.novabank.lambda.comision.ComisionHandler::handleRequest"
$roleArn = "arn:aws:iam::000000000000:role/lambda-role"

$env:AWS_ACCESS_KEY_ID = "test"
$env:AWS_SECRET_ACCESS_KEY = "test"
$env:AWS_DEFAULT_REGION = $Region

Push-Location $repoRoot

try {
    Write-Host "[INFO] Construyendo Lambda..." -ForegroundColor Cyan
    mvn -pl comision-lambda package

    if (-not (Test-Path $jarPath)) {
        throw "No se ha encontrado el JAR generado: $jarPath"
    }

    Write-Host "[INFO] Comprobando conexión con LocalStack..." -ForegroundColor Cyan

    aws --endpoint-url $EndpointUrl lambda list-functions `
        --region $Region > $null

    if ($LASTEXITCODE -ne 0) {
        throw "No se puede conectar con LocalStack en $EndpointUrl."
    }

    Write-Host "[INFO] Comprobando si existe la función $FunctionName..." -ForegroundColor Cyan

    $existingFunction = aws --endpoint-url $EndpointUrl lambda list-functions `
        --region $Region `
        --query "Functions[?FunctionName=='$FunctionName'].FunctionName | [0]" `
        --output text

    if ($existingFunction -eq $FunctionName) {
        Write-Host "[INFO] La función existe. Actualizando código..." -ForegroundColor Cyan

        aws --endpoint-url $EndpointUrl lambda update-function-code `
            --function-name $FunctionName `
            --zip-file "fileb://$jarPath" `
            --region $Region
    }
    else {
        Write-Host "[INFO] La función no existe. Creando función..." -ForegroundColor Cyan

        aws --endpoint-url $EndpointUrl lambda create-function `
            --function-name $FunctionName `
            --runtime java17 `
            --handler $handler `
            --role $roleArn `
            --zip-file "fileb://$jarPath" `
            --region $Region
    }

    Write-Host "[OK] Lambda $FunctionName desplegada en LocalStack." -ForegroundColor Green
}
finally {
    Pop-Location
}