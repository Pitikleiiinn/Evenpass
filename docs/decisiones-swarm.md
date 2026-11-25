# Decisiones técnicas del clúster Docker Swarm (IE9)

Contexto: EventPass recibe picos muy fuertes al abrir una preventa y luego queda casi sin tráfico.
El clúster debe crecer rápido en esos picos, seguir funcionando si cae un nodo o un contenedor y ser
fácil de actualizar y operar por un equipo pequeño.

## Escalabilidad

| Decisión (dónde está) | Por qué responde al requisito |
|---|---|
| **Servicios *stateless*** (JWT sin sesión en servidor; el estado vive en PostgreSQL y SQS) | Cualquier réplica puede atender cualquier petición, así que escalar es solo `docker service scale`, sin tocar el código. |
| **2 réplicas iniciales por microservicio** (`deploy.replicas`) y script `swarm-scale.sh` | Capacidad base y margen para subir a N réplicas durante la preventa y bajar después. Ej.: `swarm-scale.sh ordenes-service 5`. |
| **Routing mesh + red overlay** | El puerto publicado (8083…) responde en todos los nodos y reparte la carga entre las réplicas, estén donde estén. |
| **Cola SQS entre `ordenes-service` y la emisión** | La compra se acepta en milisegundos aunque la emisión vaya más lento; la cola absorbe el pico y la Lambda procesa a su ritmo (concurrencia máxima configurable). |
| **Límites de recursos** (`resources.limits`: 0,5 CPU / 512 MB) | Evita que una réplica descontrolada deje sin recursos a las demás del mismo nodo y hace predecible cuántas réplicas caben por nodo. |
| **Descuento de aforo atómico en la BD** | Al escalar réplicas y Lambdas concurrentes no hay sobreventa: la consistencia no depende de cuántas instancias haya. |

## Disponibilidad

| Decisión | Por qué |
|---|---|
| **Múltiples réplicas por servicio** | Si cae un contenedor o un nodo, Swarm reprograma las tareas y las otras réplicas siguen respondiendo. |
| **`restart_policy: on-failure`** con reintentos y espera | Recupera servicios que arrancan antes que PostgreSQL o que fallan puntualmente, sin intervención manual. |
| **`HEALTHCHECK` en cada imagen** (`/actuator/health`) | Swarm solo envía tráfico a réplicas sanas y reemplaza las que dejan de responder. |
| **`update_config.order: start-first` + `failure_action: rollback`** | Los despliegues del pipeline levantan la réplica nueva antes de retirar la antigua (sin corte) y, si la nueva falla, vuelven solas a la versión anterior. |
| **Número impar de managers (3 o 5) en producción** | El plano de control usa quórum Raft: con 3 managers tolera la caída de 1. (En la demo basta 1 manager + 1 worker; el README explica cómo agregar más.) |
| **Reintentos + DLQ en SQS y Lambda idempotente** | Si un microservicio no responde, el mensaje vuelve a la cola y se reintenta; tras 5 fallos queda en la DLQ sin perderse. |
| **`postgres` fijado al manager** (`placement.constraints`) | Su volumen es local; fijarlo evita que Swarm lo mueva a un nodo sin datos. |

**Limitación reconocida:** PostgreSQL corre como una sola instancia, que es el punto único de falla del
sistema. En producción se reemplazaría por un servicio administrado con alta disponibilidad (por ejemplo
Amazon RDS Multi-AZ); con las variables `DB_URL`, `DB_USER` y `DB_PASSWORD` basta con apuntar los
servicios a él.

## Mantenibilidad

| Decisión | Por qué |
|---|---|
| **Un único archivo de definición** (`docker-compose.yml`) compatible con Swarm y con Compose, más un *override* local | La misma definición sirve en el PC del desarrollador y en el clúster; solo cambia de dónde salen las imágenes. |
| **Imágenes versionadas por commit** (`TAG=${{ github.sha }}`) en GHCR | Trazabilidad: se sabe exactamente qué commit corre y se puede volver atrás con `docker service rollback`. |
| **Pipeline que provisiona y despliega** (CloudFormation + `docker stack deploy`) | Infraestructura reproducible y sin pasos manuales: el entorno se reconstruye desde el repositorio. |
| **Configuración por variables de entorno** (`.env.example`) | Los mismos artefactos funcionan en local y nube; los secretos no están en el código. |
| **Una base de datos por microservicio** | Cada equipo puede evolucionar su esquema sin romper a los demás (bajo acoplamiento). |
| **Scripts de operación** (`swarm-init`, `swarm-deploy`, `swarm-scale`, `swarm-status`) | Procedimientos repetibles y documentados en lugar de comandos memorizados. |
| **Imágenes livianas, multi-etapa y sin root** | Menos superficie de ataque, descargas rápidas al reprogramar tareas en otro nodo. |

## Mejoras futuras (no incluidas)

* Usar *Docker secrets* de Swarm en lugar de variables de entorno para las claves.
* Escalado automático: Swarm no autoescala por sí solo; se podría automatizar con una alarma de
  CloudWatch que invoque `docker service scale`.
* TLS en el API Gateway con dominio propio y autorizador JWT (el HTTP API nativo requiere un emisor OIDC;
  por eso el JWT HS256 se valida en cada microservicio).
