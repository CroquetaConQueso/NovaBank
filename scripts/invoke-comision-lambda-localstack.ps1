param(
    [string]$FunctionName = "novabank-comision",
    [string]$EndpointUrl = "http://localhost:4566",
    [string]$Region = "eu-west-1",
    [string]$OutputFile = "comision-lambda-response.json"
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$payloadPath = Join-Path $env:TEMP "novabank-comision-payload.json"
$responsePath = Join-Path $repoRoot $OutputFile

@'
{
  "importeEuros": 1000,
  "paisDestino": "US",
  "tipoCliente": "EMPRESA"
}
'@ | Set-Content -Encoding UTF8 -Path $payloadPath

$env:AWS_ACCESS_KEY_ID = "test"
$env:AWS_SECRET_ACCESS_KEY = "test"
$env:AWS_DEFAULT_REGION = $Region

aws --endpoint-url $EndpointUrl lambda invoke `
    --function-name $FunctionName `
    --payload "fileb://$payloadPath" `
    --cli-binary-format raw-in-base64-out `
    --region $Region `
    $responsePath

Get-Content $responsePath
