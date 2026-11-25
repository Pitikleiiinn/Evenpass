# EventPass - Evaluación Parcial 3 (JVY0101)

Plataforma de venta de entradas para conciertos y eventos universitarios, construida con
microservicios Java (Spring Boot), contenedores Docker, Docker Swarm, pipeline CI/CD en
GitHub Actions y una arquitectura serverless en AWS (API Gateway + SQS + Lambda).

> Este README se completa a lo largo del desarrollo. Ver `docs/` para arquitectura, decisiones
> técnicas, guion del video y pendientes.

## Estructura

```
auth-service/      Autenticación y usuarios (JWT, roles COMPRADOR / STAFF)
eventos-service/   Eventos y aforo disponible
ordenes-service/   Recepción de órdenes y publicación en SQS
tickets-service/   Emisión de códigos de acceso y validación en puerta
lambda-emision/    Función AWS Lambda (FaaS) que procesa la cola SQS
infra/             Infraestructura como código (CloudFormation)
scripts/           Scripts de Swarm, escalado y pruebas end-to-end
docs/              Documentación
```

## Docker local

```bash
cp .env.example .env
docker compose -f docker-compose.yml -f docker-compose.local.yml up --build -d
docker compose -f docker-compose.yml -f docker-compose.local.yml ps
```

| Servicio          | URL local              |
|-------------------|------------------------|
| auth-service      | http://localhost:8081  |
| eventos-service   | http://localhost:8082  |
| ordenes-service   | http://localhost:8083  |
| tickets-service   | http://localhost:8084  |
| ElasticMQ (SQS)   | http://localhost:9324  |

Cada servicio expone `GET /actuator/health`. Usuarios de prueba: `comprador@eventpass.cl`
(`Comprador123!`) y `staff@eventpass.cl` (`Staff123!`).

## Docker Swarm (IE7 / IE8)

`docker-compose.yml` es el archivo de definición del stack: 4 microservicios con 2 réplicas cada
uno, PostgreSQL, red overlay, actualización progresiva (`start-first`) con rollback automático y
límites de recursos.

### 1. Crear el clúster (nodo manager)

```bash
./scripts/swarm-init.sh <IP-DEL-MANAGER>
```

El script ejecuta `docker swarm init --advertise-addr <IP>` e imprime los comandos de unión.

### 2. Agregar un nodo worker

En la otra máquina (debe alcanzar el puerto 2377/tcp del manager):

```bash
docker swarm join --token <TOKEN-WORKER> <IP-DEL-MANAGER>:2377
```

El token se vuelve a consultar en el manager con `docker swarm join-token worker`.

### 3. Agregar un nodo manager adicional

```bash
# en el manager actual: obtener el token
docker swarm join-token manager
# en la nueva máquina:
docker swarm join --token <TOKEN-MANAGER> <IP-DEL-MANAGER>:2377
```

Para alta disponibilidad del plano de control usa un número impar de managers (3 o 5).
Puertos que deben estar abiertos entre nodos: `2377/tcp`, `7946/tcp+udp` y `4789/udp`.

### 4. Desplegar los servicios distribuidos

```bash
./scripts/swarm-deploy.sh            # = docker stack deploy -c docker-compose.yml eventpass
./scripts/swarm-status.sh            # nodos, servicios y en qué nodo corre cada réplica
```

### 5. Escalar según la carga

```bash
./scripts/swarm-scale.sh ordenes-service 5   # hacia arriba (preventa abierta)
./scripts/swarm-scale.sh ordenes-service 1   # hacia abajo
```

Equivale a `docker service scale eventpass_ordenes-service=5`. Los servicios son *stateless* (el
estado vive en PostgreSQL y en la cola), por lo que escalan sin cambios de código.

## Endpoints principales

| Método y ruta | Rol | Descripción |
|---|---|---|
| `POST /auth/registro` | público | Registra un comprador |
| `POST /auth/login` | público | Devuelve el JWT |
| `GET /eventos` · `GET /eventos/{id}` | COMPRADOR, STAFF | Eventos y aforo disponible |
| `POST /eventos` | STAFF | Crea un evento |
| `POST /ordenes` | COMPRADOR | Acepta la compra → `202` `PENDIENTE_EMISION` |
| `GET /ordenes/{id}` · `GET /ordenes/mis-ordenes` | COMPRADOR | Estado de la orden (`EMITIDA` / `RECHAZADA`) |
| `GET /tickets/mis-tickets` | COMPRADOR | Códigos de acceso emitidos |
| `POST /tickets/validar` | STAFF | Valida un código en puerta (uso único) |

Rutas `/internal/**`: solo para la función Lambda (`X-Internal-Key`); no se publican en el API Gateway.
Colección de Postman lista para importar en `docs/postman/`.

## Función serverless (FaaS) - IE10

`lambda-emision/` contiene la función `EmisionTicketsHandler` (Java 21), disparada por la cola SQS.
Ver el diseño completo en [`docs/arquitectura.md`](docs/arquitectura.md).

```bash
# Pruebas unitarias de la función (sin red, con servicios simulados)
cd lambda-emision && mvn test

# Ejecutarla en local contra ElasticMQ (ya incluida en docker-compose.local.yml)
docker compose -f docker-compose.yml -f docker-compose.local.yml up --build -d

# Desplegarla en AWS (SQS + Lambda + trigger + API Gateway)
export ARTIFACT_BUCKET=eventpass-artefactos-<tu-sufijo-unico> BACKEND_HOST=<IP-o-DNS-del-manager> \
       GATEWAY_KEY=<16+ caracteres> INTERNAL_API_KEY=<16+ caracteres>
./scripts/aws-provision.sh

# Probarla en AWS enviando un mensaje a la cola (la orden debe existir) o con el flujo completo:
API_URL=https://<id>.execute-api.<region>.amazonaws.com ./scripts/e2e-test.sh
```

Variables de entorno de la Lambda: `EVENTOS_URL`, `ORDENES_URL`, `TICKETS_URL`, `INTERNAL_API_KEY`
(las define la plantilla `infra/cloudformation.yml`).

## Pipeline CI/CD - IE4 / IE5 / IE6

`.github/workflows/ci-cd.yml` (se ejecuta en cada push a `main`):

1. **test**: `mvn verify` en los 4 microservicios y la Lambda.
2. **docker**: construye y publica las imágenes en GitHub Container Registry (`ghcr.io`).
3. **provision-cloud**: `scripts/aws-provision.sh` crea/actualiza la cola SQS (+DLQ), la Lambda con su
   trigger y el API Gateway mediante CloudFormation (infraestructura como código).
4. **deploy**: `docker stack deploy` contra el manager del Swarm por SSH.
5. **smoke-test**: ejecuta `scripts/e2e-test.sh` a través del API Gateway.

### Secretos y variables necesarios (Settings → Secrets and variables → Actions)

| Nombre | Para qué |
|---|---|
| `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_SESSION_TOKEN` | Credenciales AWS (el token solo si usas AWS Academy) |
| `ARTIFACT_BUCKET` | Bucket S3 (nombre único global) para el `.jar` de la Lambda |
| `LAMBDA_ROLE_ARN` | (Opcional) rol existente, p. ej. `LabRole` en AWS Academy |
| `EC2_HOST`, `EC2_USER`, `EC2_SSH_KEY` | Nodo manager del Swarm y llave SSH privada |
| `JWT_SECRET`, `INTERNAL_API_KEY`, `GATEWAY_KEY` | Secretos de la aplicación (≥ 32 / 16 / 16 caracteres) |
| `DB_PASSWORD`, `SEED_COMPRADOR_PASSWORD`, `SEED_STAFF_PASSWORD` | Base de datos y usuarios demo |
| `GHCR_PAT` | (Opcional) token con `read:packages` si los paquetes de GHCR son privados |
| Variable `AWS_REGION` | (Opcional) por defecto `us-east-1` |

> Los secretos **nunca** se escriben en el repositorio (`.env` está en `.gitignore`).

## Pruebas

```bash
(cd auth-service    && mvn test)
(cd eventos-service && mvn test)
(cd ordenes-service && mvn test)
(cd tickets-service && mvn test)
(cd lambda-emision  && mvn test)
./scripts/e2e-test.sh        # sistema completo levantado (local o nube)
```

## Documentación

* [`docs/arquitectura.md`](docs/arquitectura.md) - diagramas y garantías de consistencia
* [`docs/decisiones-swarm.md`](docs/decisiones-swarm.md) - argumentación técnica del clúster (IE9)
* [`docs/guion-video.md`](docs/guion-video.md) - guion de la presentación en video (IE2-IE17)
* [`docs/pendientes-companera.md`](docs/pendientes-companera.md) - lo que falta hacer manualmente
