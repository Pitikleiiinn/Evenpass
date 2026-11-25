#!/usr/bin/env bash
# Ajusta dinamicamente el numero de replicas de un servicio del stack.
#
# Uso: ./scripts/swarm-scale.sh <servicio> <replicas>
# Ej.: ./scripts/swarm-scale.sh ordenes-service 5     (escalar hacia arriba)
#      ./scripts/swarm-scale.sh ordenes-service 1     (escalar hacia abajo)
set -euo pipefail

SERVICIO="${1:-}"
REPLICAS="${2:-}"
STACK="${STACK:-eventpass}"
if [ -z "$SERVICIO" ] || [ -z "$REPLICAS" ]; then
  echo "Uso: $0 <servicio> <replicas>   (servicios: auth-service eventos-service ordenes-service tickets-service)"
  exit 1
fi

echo "Antes:"; docker service ls --filter "name=${STACK}_${SERVICIO}"
docker service scale "${STACK}_${SERVICIO}=${REPLICAS}"
echo
echo "Despues:"; docker service ls --filter "name=${STACK}_${SERVICIO}"
echo
echo "Distribucion de las tareas en los nodos:"
docker service ps "${STACK}_${SERVICIO}" --filter "desired-state=running" \
  --format 'table {{.Name}}\t{{.Node}}\t{{.CurrentState}}'
