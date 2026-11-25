#!/usr/bin/env bash
# Muestra el estado del cluster: nodos, servicios y tareas del stack.
set -euo pipefail
STACK="${STACK:-eventpass}"

echo "=== Nodos ==="; docker node ls
echo; echo "=== Servicios ==="; docker stack services "$STACK"
echo; echo "=== Tareas (en que nodo corre cada replica) ==="
docker stack ps "$STACK" --filter "desired-state=running" \
  --format 'table {{.Name}}\t{{.Image}}\t{{.Node}}\t{{.CurrentState}}'
