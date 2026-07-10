param(
    [string]$FunctionName = "novabank-comision",
    [string]$EndpointUrl = "http://localhost:4566",
    [string]$Region = "eu-west-1"
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$jarPath = Join-Path $repoRoot "comision-lambda\target\comision-lambda-4.0-SNAPSHOT-aws.jar"
$handler = "com.novabank.lambda.comision.ComisionHandler::handleRequest"
$roleArn = "arn:aws:iam::000000000000:role/lambda-role"

$env:AWS_ACCESS_KEY_ID = "test"
$env:AWS_SECRET_ACCESS_KEY = "test"
$env:AWS_DEFAULT_REGION = $Region

Push-Location $repoRoot
try {
    mvn -pl comision-lambda package

    aws --endpoint-url $EndpointUrl lambda get-function `
        --function-name $FunctionName `
        --region $Region *> $null

    if ($LASTEXITCODE -eq 0) {
        aws --endpoint-url $EndpointUrl lambda update-function-code `
            --function-name $FunctionName `
            --zip-file "fileb://$jarPath" `
            --region $Region
    } else {
        aws --endpoint-url $EndpointUrl lambda create-function `
            --function-name $FunctionName `
            --runtime java17 `
            --handler $handler `
            --role $roleArn `
            --zip-file "fileb://$jarPath" `
            --region $Region
    }
} finally {
    Pop-Location
}
