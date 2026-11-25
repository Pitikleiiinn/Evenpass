# Pendientes (lo que NO se pudo hacer desde el entorno de desarrollo asistido)

El código, los Dockerfiles, el docker-compose/Swarm, los scripts, la plantilla de CloudFormation y el
pipeline están listos en el repositorio. Lo siguiente requiere cuentas, máquinas o personas, y queda
para completar a mano:

## A. Verificación técnica (hacerlo primero)

- [ ] **Compilar y ejecutar las pruebas** en un equipo con acceso a Maven Central:
      `mvn test` dentro de cada carpeta (`auth-service`, `eventos-service`, `ordenes-service`,
      `tickets-service`, `lambda-emision`). El código se escribió sin poder compilarlo en el entorno
      de trabajo, por lo que puede haber pequeños errores de compilación que corregir.
- [ ] **Levantar todo en local** con `docker compose -f docker-compose.yml -f docker-compose.local.yml up --build -d`
      y correr `./scripts/e2e-test.sh` (necesita `jq`). Confirmar que las imágenes construyen
      (en particular el paso `java -Djarmode=tools ... extract --layers` de los Dockerfiles).
- [ ] **Aplicar las correcciones del docente de la evaluación parcial anterior.** El repositorio estaba
      vacío, así que los microservicios se construyeron desde el caso EventPass; el código de la EP2 y la
      retroalimentación del docente no estaban disponibles. Revisar los comentarios y ajustar.

## B. Cuenta y recursos de AWS

- [ ] Crear 2 instancias **EC2** (1 manager + 1 worker) con Docker instalado. Grupos de seguridad:
      `8081-8084/tcp` abiertos (el API Gateway necesita llegar a los microservicios), y entre los nodos
      `2377/tcp`, `7946/tcp+udp`, `4789/udp`. SSH (`22/tcp`) para el pipeline.
- [ ] Rol/perfil IAM de la EC2 manager con permiso `sqs:SendMessage` sobre la cola (la política
      `OrdenesPublisherPolicy` sale en las salidas de CloudFormation) para que `ordenes-service`
      publique en SQS sin llaves.
- [ ] Crear el bucket S3 para el `.jar` de la Lambda (nombre único global).
- [ ] Si usan **AWS Academy**: copiar las credenciales temporales (incluye `AWS_SESSION_TOKEN`, caducan) y
      definir `LAMBDA_ROLE_ARN` con el ARN de `LabRole`.
- [ ] Verificar la región (por defecto `us-east-1`) y que Lambda `java21` esté disponible.

## C. GitHub

- [ ] Crear los **secretos** del pipeline (tabla en el README) y el *environment* `produccion`.
- [ ] En el manager: `./scripts/swarm-init.sh <IP>` y unir el worker con el comando que imprime.
- [ ] Hacer público el paquete de GHCR o crear `GHCR_PAT` (`read:packages`).
- [ ] Hacer un push a `main` y confirmar que los 5 jobs terminan en verde (capturas para el video).
- [ ] Si el docente exige **un repositorio por componente** ("varios enlaces a Github"), separar las
      carpetas en repositorios distintos o consultar si basta con este monorepo.

## D. Entrega

- [ ] **Grabar el video** (3 a 8 min, voz de ambos integrantes, audio limpio) siguiendo `docs/guion-video.md`.
- [ ] Subir los enlaces de GitHub y el video al **AVA** y enviar copia al correo del docente.

## Mapa rápido: indicador → dónde está

| IE | Evidencia en el repositorio | Estado |
|---|---|---|
| IE1 | `*/Dockerfile` | Listo (probar build) |
| IE2 | Video + `docs/postman` | Pendiente: grabar |
| IE3 | `docker-compose.yml` | Listo (probar en Swarm) |
| IE4 | `.github/workflows/ci-cd.yml` | Listo (probar ejecución) |
| IE5 | `scripts/aws-provision.sh` + `infra/cloudformation.yml` | Listo (requiere AWS) |
| IE6 | Ejecución del pipeline | Pendiente: requiere AWS/EC2/secretos |
| IE7 | `scripts/swarm-*.sh` + README | Listo (probar con 2 nodos) |
| IE8 | `scripts/swarm-scale.sh` | Pendiente: demostrar en video |
| IE9 | `docs/decisiones-swarm.md` | Listo; falta exponerlo en el video |
| IE10 | `lambda-emision/` + README | Listo |
| IE11, IE14 | `infra/cloudformation.yml` (SQS) + productor/consumidor | Pendiente: mostrar en consola AWS |
| IE12 | API Gateway en `infra/cloudformation.yml` | Pendiente: desplegar y mostrar |
| IE13 | Lambda ↔ microservicios | Pendiente: demostrar en AWS |
| IE15 | `scripts/e2e-test.sh` | Pendiente: ejecutar contra la nube |
| IE16, IE17 | Video | Pendiente: grabar |
