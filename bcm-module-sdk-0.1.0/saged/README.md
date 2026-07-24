# Module skeleton (SDK 0.1.0)

Copy or rename this project into your domain module.

1. Change package `br.gov.crateus.bcm.example` → `br.gov.crateus.bcm.<moduleId>`
2. Change schema / API prefix to match `<moduleId>`
3. Replace `ExampleController` with your domain API
4. Replace the Flyway script under `db/module-migration/`
5. Run the Dev Host: `./gradlew :bcm-dev-host:bootRun`

See `platform-docs/interno/guia-usar-sdk-bcm.md`.
