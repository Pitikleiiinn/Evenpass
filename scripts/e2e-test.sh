#!/usr/bin/env bash
# Prueba de extremo a extremo del flujo completo (IE15):
#   peticion externa -> (API Gateway) -> microservicios -> cola SQS -> funcion Lambda -> tickets
# y valida: respuesta inmediata, procesamiento asincrono, SIN sobreventa de aforo bajo carga
# concurrente, codigos de acceso, uso unico del codigo y control de roles.
#
# Requisitos: bash, curl, jq.
#
# Local (puertos directos):   ./scripts/e2e-test.sh
# En la nube (API Gateway):   API_URL=https://xxxx.execute-api.us-east-1.amazonaws.com ./scripts/e2e-test.sh
# Opciones: AFORO=10 PEDIDOS=25 CONCURRENCIA=25 TIMEOUT=90
set -uo pipefail

command -v jq >/dev/null || { echo "Necesitas instalar jq"; exit 1; }
command -v curl >/dev/null || { echo "Necesitas instalar curl"; exit 1; }

if [ -n "${API_URL:-}" ]; then
  AUTH_URL="$API_URL"; EVENTOS_URL="$API_URL"; ORDENES_URL="$API_URL"; TICKETS_URL="$API_URL"
else
  AUTH_URL="${AUTH_URL:-http://localhost:8081}"
  EVENTOS_URL="${EVENTOS_URL:-http://localhost:8082}"
  ORDENES_URL="${ORDENES_URL:-http://localhost:8083}"
  TICKETS_URL="${TICKETS_URL:-http://localhost:8084}"
fi
COMPRADOR_EMAIL="${COMPRADOR_EMAIL:-comprador@eventpass.cl}"
COMPRADOR_PASS="${COMPRADOR_PASS:-Comprador123!}"
STAFF_EMAIL="${STAFF_EMAIL:-staff@eventpass.cl}"
STAFF_PASS="${STAFF_PASS:-Staff123!}"
AFORO="${AFORO:-10}"
PEDIDOS="${PEDIDOS:-25}"
CONCURRENCIA="${CONCURRENCIA:-25}"
TIMEOUT="${TIMEOUT:-90}"

CUERPO="$(mktemp)"; SALIDA="$(mktemp)"
trap 'rm -f "$CUERPO" "$SALIDA"' EXIT
FALLOS=0

ok()    { echo "  [OK]    $1"; }
fallo() { echo "  [FALLO] $1"; FALLOS=$((FALLOS + 1)); }
iguales() { if [ "$2" = "$3" ]; then ok "$1 (= $3)"; else fallo "$1 (esperado: $2, obtenido: $3)"; fi; }
paso()  { echo; echo "== $1"; }

# http METODO URL [TOKEN] [JSON]  -> imprime el codigo HTTP; el cuerpo queda en $CUERPO
http() {
  local metodo="$1" url="$2" token="${3:-}" datos="${4:-}"
  local args=(-s -o "$CUERPO" -w '%{http_code}' -X "$metodo" "$url" -H 'Content-Type: application/json')
  [ -n "$token" ] && args+=(-H "Authorization: Bearer $token")
  [ -n "$datos" ] && args+=(-d "$datos")
  curl "${args[@]}"
}

login() {
  local st; st="$(http POST "$AUTH_URL/auth/login" "" "{\"email\":\"$1\",\"password\":\"$2\"}")"
  if [ "$st" != "200" ]; then echo "No se pudo iniciar sesion como $1 (HTTP $st)"; cat "$CUERPO"; exit 1; fi
  jq -r .token "$CUERPO"
}

fecha_futura() {
  date -u -d "+30 days" +%Y-%m-%dT20:00:00 2>/dev/null || date -u -v+30d +%Y-%m-%dT20:00:00
}

paso "1. Autenticacion y autorizacion por rol"
TOKEN_C="$(login "$COMPRADOR_EMAIL" "$COMPRADOR_PASS")"; ok "login comprador"
TOKEN_S="$(login "$STAFF_EMAIL" "$STAFF_PASS")";         ok "login staff"
iguales "GET /eventos sin token -> 401" 401 "$(http GET "$EVENTOS_URL/eventos")"
iguales "comprador no puede crear eventos -> 403" 403 \
  "$(http POST "$EVENTOS_URL/eventos" "$TOKEN_C" '{"nombre":"x","lugar":"x","fecha":"2030-01-01T20:00:00","aforo":1,"precio":1}')"

paso "2. Staff crea un evento con aforo ${AFORO}"
NOMBRE="Preventa E2E $(date +%s)"
ST="$(http POST "$EVENTOS_URL/eventos" "$TOKEN_S" \
  "{\"nombre\":\"$NOMBRE\",\"lugar\":\"Gimnasio\",\"fecha\":\"$(fecha_futura)\",\"aforo\":$AFORO,\"precio\":10000}")"
iguales "POST /eventos -> 201" 201 "$ST"
EVENTO_ID="$(jq -r .id "$CUERPO")"
iguales "aforo disponible inicial" "$AFORO" "$(jq -r .aforoDisponible "$CUERPO")"

paso "3. Comprador consulta eventos"
iguales "GET /eventos -> 200" 200 "$(http GET "$EVENTOS_URL/eventos" "$TOKEN_C")"

paso "4. ${PEDIDOS} compras SIMULTANEAS de 1 entrada (preventa)"
export ORDENES_URL TOKEN_C EVENTO_ID
INICIO="$(date +%s%N 2>/dev/null || echo 0)"
seq 1 "$PEDIDOS" | xargs -P "$CONCURRENCIA" -I{} sh -c \
  'curl -s -X POST "$ORDENES_URL/ordenes" -H "Authorization: Bearer $TOKEN_C" -H "Content-Type: application/json" -d "{\"eventoId\":$EVENTO_ID,\"cantidad\":1}"; echo' > "$SALIDA"
FIN="$(date +%s%N 2>/dev/null || echo 0)"
ACEPTADAS="$(jq -r 'select(.estado=="PENDIENTE_EMISION") | .id' "$SALIDA" 2>/dev/null | wc -l | tr -d ' ')"
echo "  Tiempo total de aceptacion: $(( (FIN - INICIO) / 1000000 )) ms"
iguales "ordenes aceptadas de inmediato (PENDIENTE_EMISION)" "$PEDIDOS" "$ACEPTADAS"

paso "5. Procesamiento asincrono (SQS -> Lambda -> eventos/tickets)"
ESPERADAS_EMITIDAS=$(( ACEPTADAS < AFORO ? ACEPTADAS : AFORO ))
PENDIENTES=-1
for _ in $(seq 1 "$TIMEOUT"); do
  http GET "$ORDENES_URL/ordenes/mis-ordenes" "$TOKEN_C" > /dev/null
  PENDIENTES="$(jq --argjson e "$EVENTO_ID" '[.[] | select(.eventoId==$e and .estado=="PENDIENTE_EMISION")] | length' "$CUERPO")"
  [ "$PENDIENTES" = "0" ] && break
  sleep 1
done
iguales "ordenes aun pendientes tras la espera" 0 "$PENDIENTES"
EMITIDAS="$(jq --argjson e "$EVENTO_ID" '[.[] | select(.eventoId==$e and .estado=="EMITIDA")] | length' "$CUERPO")"
RECHAZADAS="$(jq --argjson e "$EVENTO_ID" '[.[] | select(.eventoId==$e and .estado=="RECHAZADA")] | length' "$CUERPO")"
iguales "ordenes EMITIDAS (no mas que el aforo)" "$ESPERADAS_EMITIDAS" "$EMITIDAS"
iguales "ordenes RECHAZADAS por falta de cupos" "$(( ACEPTADAS - ESPERADAS_EMITIDAS ))" "$RECHAZADAS"
http GET "$EVENTOS_URL/eventos/$EVENTO_ID" "$TOKEN_C" > /dev/null
iguales "aforo disponible final (sin sobreventa)" "$(( AFORO - ESPERADAS_EMITIDAS ))" "$(jq -r .aforoDisponible "$CUERPO")"

paso "6. Comprador consulta sus codigos de acceso"
iguales "GET /tickets/mis-tickets -> 200" 200 "$(http GET "$TICKETS_URL/tickets/mis-tickets" "$TOKEN_C")"
TICKETS_EVENTO="$(jq --argjson e "$EVENTO_ID" '[.[] | select(.eventoId==$e)] | length' "$CUERPO")"
iguales "tickets generados para el evento" "$ESPERADAS_EMITIDAS" "$TICKETS_EVENTO"
CODIGO="$(jq -r --argjson e "$EVENTO_ID" '[.[] | select(.eventoId==$e)][0].codigo' "$CUERPO")"
echo "  Codigo de ejemplo: $CODIGO"

paso "7. Validacion en puerta (uso unico)"
iguales "comprador no puede validar -> 403" 403 "$(http POST "$TICKETS_URL/tickets/validar" "$TOKEN_C" "{\"codigo\":\"$CODIGO\"}")"
iguales "staff valida el codigo -> 200" 200 "$(http POST "$TICKETS_URL/tickets/validar" "$TOKEN_S" "{\"codigo\":\"$CODIGO\"}")"
iguales "segundo uso del mismo codigo -> 409" 409 "$(http POST "$TICKETS_URL/tickets/validar" "$TOKEN_S" "{\"codigo\":\"$CODIGO\"}")"
iguales "codigo inexistente -> 404" 404 "$(http POST "$TICKETS_URL/tickets/validar" "$TOKEN_S" '{"codigo":"EP-ZZZZ-ZZZZ-ZZZZ"}')"

echo
if [ "$FALLOS" -eq 0 ]; then
  echo "RESULTADO: TODAS LAS VALIDACIONES PASARON"
else
  echo "RESULTADO: $FALLOS validacion(es) fallaron"
fi
exit "$FALLOS"
