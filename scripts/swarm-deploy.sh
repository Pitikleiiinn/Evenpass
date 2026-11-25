#!/usr/bin/env bash
# Despliega (o actualiza) el stack "eventpass" en el Swarm. Ejecutar en un nodo MANAGER.
#
# Uso: ./scripts/swarm-deploy.sh [archivo-compose]
# Lee las variables de un archivo .env si existe (REGISTRY, TAG, JWT_SECRET, SQS_QUEUE_URL, ...).
set -euo pipefail

COMPOSE_FILE="${1:-docker-compose.yml}"
STACK="${STACK:-eventpass}"

if [ -f .env ]; then
  set -a; . ./.env; set +a
fi

docker stack deploy --compose-file "$COMPOSE_FILE" --with-registry-auth "$STACK"

echo
echo "Esperando a que los servicios queden listos (Ctrl+C para salir)..."
for _ in $(seq 1 30); do
  docker stack services "$STACK"
  PENDIENTES="$(docker stack services "$STACK" --format '{{.Replicas}}' | awk -F'[/ ]' '$1!=$2' | wc -l)"
  [ "$PENDIENTES" -eq 0 ] && { echo "Todos los servicios estan en su numero de replicas deseado."; exit 0; }
  sleep 10
done
echo "Aun hay servicios iniciando; revisa con: docker stack ps $STACK --no-trunc"
