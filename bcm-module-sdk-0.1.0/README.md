# BCM Module SDK 0.1.0

Build kit only. Documentation lives in the Seplati GitHub repo (platform-docs), not in this zip.

## Quick start

```bash
docker compose up -d
./gradlew :bcm-dev-host:bootRun
```

- API: http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui.html
- Ping: GET /api/v1/system/ping
- Example: GET /api/v1/example/hello
- Keycloak: http://localhost:8180 (admin/admin), realm bcm-sdk
- Users: dev-admin/dev-admin (ADMIN), dev-user/dev-user

## Layout

| Path | Role |
|---|---|
| bcm-sdk-api/ | Java contracts |
| bcm-dev-host/ | Local host (JWT + Flyway + outbox) |
| module-skeleton/ | Starter module example |
| docker-compose.yml | Postgres + Keycloak DEV |

## Deliver back to Seplati

Module source + Flyway + SPA (if any). Not this zip. Not a PR on the BCM monorepo.
