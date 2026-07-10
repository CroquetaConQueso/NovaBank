#!/bin/sh
set -eu

BUCKET_NAME="${NOVABANK_JUSTIFICANTES_BUCKET:-novabank-justificantes}"
ENDPOINT_URL="${LOCALSTACK_ENDPOINT:-http://localhost:4566}"

awslocal s3api head-bucket --bucket "$BUCKET_NAME" --endpoint-url "$ENDPOINT_URL" >/dev/null 2>&1 \
  || awslocal s3api create-bucket \
    --bucket "$BUCKET_NAME" \
    --create-bucket-configuration LocationConstraint=eu-west-1 \
    --endpoint-url "$ENDPOINT_URL"
