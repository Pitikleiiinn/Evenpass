#!/usr/bin/env bash
# Provisiona y configura los servicios de nube que usa el sistema (IE5):
#   1. compila la funcion Lambda (fat jar)
#   2. crea el bucket S3 de artefactos (si no existe) y sube el jar
#   3. despliega infra/cloudformation.yml -> cola SQS + DLQ, Lambda + trigger, API Gateway
#   4. imprime/exporta las salidas (URL del API, URL de la cola)
#
# Lo ejecuta el pipeline (job "provision-cloud") y tambien se puede correr a mano.
#
# Variables requeridas:
#   ARTIFACT_BUCKET   bucket S3 para el .jar (se crea si no existe; nombre unico global)
#   BACKEND_HOST      IP/DNS publica del nodo Swarm con los microservicios
#   GATEWAY_KEY       clave que inyecta API Gateway (min. 16 caracteres)
#   INTERNAL_API_KEY  clave interna Lambda -> microservicios (min. 16 caracteres)
# Opcionales:
#   AWS_REGION (us-east-1), STACK_NAME (eventpass-infra), LAMBDA_ROLE_ARN (rol existente, ej. LabRole)
set -euo pipefail

: "${ARTIFACT_BUCKET:?Falta ARTIFACT_BUCKET}"
: "${BACKEND_HOST:?Falta BACKEND_HOST}"
: "${GATEWAY_KEY:?Falta GATEWAY_KEY}"
: "${INTERNAL_API_KEY:?Falta INTERNAL_API_KEY}"
AWS_REGION="${AWS_REGION:-us-east-1}"
STACK_NAME="${STACK_NAME:-eventpass-infra}"
LAMBDA_ROLE_ARN="${LAMBDA_ROLE_ARN:-}"
export AWS_DEFAULT_REGION="$AWS_REGION"

RAIZ="$(cd "$(dirname "$0")/.." && pwd)"
VERSION="$(git -C "$RAIZ" rev-parse --short HEAD 2>/dev/null || date +%s)"
CLAVE_S3="lambda/lambda-emision-${VERSION}.jar"

echo ">> 1/4 Compilando la funcion Lambda"
(cd "$RAIZ/lambda-emision" && mvn -B -q package -DskipTests)

echo ">> 2/4 Subiendo el artefacto a s3://${ARTIFACT_BUCKET}/${CLAVE_S3}"
if ! aws s3api head-bucket --bucket "$ARTIFACT_BUCKET" 2>/dev/null; then
  aws s3 mb "s3://${ARTIFACT_BUCKET}" --region "$AWS_REGION"
fi
aws s3 cp "$RAIZ/lambda-emision/target/lambda-emision.jar" "s3://${ARTIFACT_BUCKET}/${CLAVE_S3}"

echo ">> 3/4 Desplegando la infraestructura (${STACK_NAME})"
aws cloudformation deploy \
  --template-file "$RAIZ/infra/cloudformation.yml" \
  --stack-name "$STACK_NAME" \
  --capabilities CAPABILITY_IAM \
  --no-fail-on-empty-changeset \
  --parameter-overrides \
    "BackendHost=${BACKEND_HOST}" \
    "GatewayKey=${GATEWAY_KEY}" \
    "InternalApiKey=${INTERNAL_API_KEY}" \
    "LambdaCodeBucket=${ARTIFACT_BUCKET}" \
    "LambdaCodeKey=${CLAVE_S3}" \
    "LambdaRoleArn=${LAMBDA_ROLE_ARN}"

salida() {
  aws cloudformation describe-stacks --stack-name "$STACK_NAME" \
    --query "Stacks[0].Outputs[?OutputKey=='$1'].OutputValue" --output text
}

echo ">> 4/4 Salidas del stack"
API_URL="$(salida ApiUrl)"
QUEUE_URL="$(salida QueueUrl)"
echo "API_URL=${API_URL}"
echo "SQS_QUEUE_URL=${QUEUE_URL}"

# Cuando corre en GitHub Actions, deja las salidas disponibles para los jobs siguientes
if [ -n "${GITHUB_OUTPUT:-}" ]; then
  {
    echo "api_url=${API_URL}"
    echo "queue_url=${QUEUE_URL}"
  } >> "$GITHUB_OUTPUT"
fi
