# Guion del video (3 a 8 minutos) - cubre IE2 a IE17

Graben con audífonos con micrófono, en un lugar sin ruido, y que **se escuche la voz de ambos**
(IE17). Estructura sugerida: introducción → desarrollo → cierre (IE16). Usen lenguaje técnico.

| Min | Qué mostrar en pantalla | Qué decir | IE |
|---|---|---|---|
| 0:00 | Diagrama de `docs/arquitectura.md` | Presentarse, explicar EventPass y la problemática de la preventa: separar aceptación de orden y emisión de ticket. | IE16 |
| 0:30 | Terminal: `docker compose -f docker-compose.yml -f docker-compose.local.yml up --build -d` y `docker compose ps` | Cómo se construyen las imágenes (Dockerfile multi-etapa, usuario no root, healthcheck) y se inician los contenedores. | IE1, IE2 |
| 1:15 | Postman (`docs/postman`): health y login de los 4 servicios; `GET /eventos` | Verificar el acceso a los endpoints de cada microservicio. | IE2 |
| 1:45 | Abrir `docker-compose.yml` | Explicar servicios, red overlay, réplicas, `update_config`, límites. Cómo levanta varias imágenes en varios contenedores. | IE3 |
| 2:15 | `./scripts/swarm-init.sh`, unir un worker, `./scripts/swarm-deploy.sh`, `./scripts/swarm-status.sh` | Despliegue con Docker Swarm: nodos manager y worker, servicios corriendo y en qué nodo. | IE3, IE7 |
| 3:00 | `./scripts/swarm-scale.sh ordenes-service 5` y luego `1`; luego `GET /eventos` | Escalado dinámico de réplicas y que el servicio sigue respondiendo. | IE8 |
| 3:40 | `docs/decisiones-swarm.md` (tabla) | Justificar decisiones: escalabilidad, disponibilidad y mantenibilidad con ejemplos del despliegue. | IE9 |
| 4:20 | Consola AWS: cola SQS `eventpass-ordenes-emision` y su DLQ | Crear/mostrar la cola; propósito: aceptar la compra de inmediato y emitir de forma asíncrona. | IE11, IE14 |
| 4:50 | Consola API Gateway: rutas, integraciones, throttling | Cómo el gateway protege el backend: solo publica 4 prefijos, inyecta `X-Gateway-Key`, `/internal` no existe. Mostrar un `403` al llamar directo al puerto del servicio. | IE12 |
| 5:30 | Consola Lambda + código de `EmisionTicketsHandler` + trigger SQS + logs de CloudWatch | Diseño de la función, cómo se conecta con eventos/tickets/órdenes, idempotencia y compensación. Ejemplo de ejecución. | IE10, IE13 |
| 6:15 | GitHub: hacer un commit/push; pestaña Actions con los jobs (test → docker → provision → deploy → e2e) | Pipeline en acción; el microservicio queda accesible en la nube. | IE4, IE5, IE6 |
| 7:00 | `API_URL=... ./scripts/e2e-test.sh` | Flujo completo con 25 compras simultáneas, aforo 10: 10 emitidas, 15 rechazadas, sin sobreventa; código usado dos veces → `409`. | IE15 |
| 7:40 | Diagrama final | Cierre: conclusiones y aprendizajes. | IE16 |

Consejo: ensayen una vez y graben por partes (cada integrante narra su sección) para que el
total quede entre 3 y 8 minutos.
