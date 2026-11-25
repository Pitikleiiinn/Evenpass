#!/usr/bin/env bash
# Inicializa el cluster Docker Swarm en ESTE equipo (nodo manager) e imprime los comandos
# para unir nodos worker y manager adicionales.
#
# Uso: ./scripts/swarm-init.sh <IP-del-manager>
set -euo pipefail

ADVERTISE_ADDR="${1:-}"
if [ -z "$ADVERTISE_ADDR" ]; then
  echo "Uso: $0 <IP-del-manager>   (ej: 192.168.1.10 o la IP privada de la instancia EC2)"
  exit 1
fi

ESTADO="$(docker info --format '{{.Swarm.LocalNodeState}}')"
if [ "$ESTADO" = "active" ]; then
  echo "Este nodo ya forma parte de un Swarm."
else
  docker swarm init --advertise-addr "$ADVERTISE_ADDR"
fi

echo
echo "=== Para agregar un nodo WORKER, ejecuta en la otra maquina: ==="
echo "docker swarm join --token $(docker swarm join-token -q worker) ${ADVERTISE_ADDR}:2377"
echo
echo "=== Para agregar un nodo MANAGER, ejecuta en la otra maquina: ==="
echo "docker swarm join --token $(docker swarm join-token -q manager) ${ADVERTISE_ADDR}:2377"
echo
docker node ls
