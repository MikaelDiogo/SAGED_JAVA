# Especificação Técnica — Módulo SAGED (extraída da doc institucional BCM)

> Conteúdo copiado da documentação oficial da plataforma
> (`https://api.pontodatec.com.br/institutional/v1/docs/index.html`), seção
> "Módulo SAGED — receita completa". Guardado aqui em arquivo local para que
> o Claude Code (sem acesso à internet) tenha o contrato técnico completo,
> sem depender de reabrir o link.

---

## 1. Identidade do módulo

| Item | Valor |
|---|---|
| Module id / pasta Gradle / schema | `saged` |
| Pacote | `br.gov.crateus.bcm.saged` |
| API | `/api/v1/saged/...` |
| SDK usado | `0.1.0` |

Convenção de nomes — sempre em inglês no código/schema/paths:

| Correto | Errado |
|---|---|
| `saged.demands` | `saged.demandas` |
| `/api/v1/saged/demands` | `/api/v1/saged/demandas` |
| `DemandService` | `ServicoDemandas` |

A UI pode mostrar "Demandas" em português; código, tabelas e paths não.

## 2. Escopo da v1

- Web Kanban + canal Telegram de teste.
- Bot é de teste (criado via BotFather pelo próprio time), não o bot oficial.
- Usar exatamente os nomes de roles/claims definidos aqui — não inventar.
- Em DEV, `department_id` e outros UUIDs de unidade são valores de teste; em
  produção mapeiam para `organization` real da plataforma.

## 3. Bootstrap do projeto (skeleton → `saged`)

```bash
cp -R module-skeleton saged
```

`settings.gradle.kts`:
```kotlin
include("bcm-sdk-api")
include("bcm-dev-host")
include("module-skeleton")
include("saged")
```

Em `bcm-dev-host/build.gradle.kts`, trocar a dependência do skeleton por:
```kotlin
implementation(project(":saged"))
```

Renomear pacote `example` → `saged`. Em `package-info.java`:
```java
@br.gov.crateus.bcm.sdk.module.BcmBusinessModule(id = "saged", displayName = "SAGED")
package br.gov.crateus.bcm.saged;
```

Smoke test inicial (controller de exemplo):
```java
@RestController
@RequestMapping("/api/v1/saged")
@Tag(name = "saged")
public class SagedHelloController {

  @GetMapping("/hello")
  public Map<String, String> hello() {
    return Map.of("module", "saged", "status", "up");
  }
}
```

```bash
./gradlew :bcm-dev-host:bootRun
curl -s http://localhost:8080/api/v1/saged/hello
```

Depois que `/api/v1/saged/...` responder, o `module-skeleton` pode ser
removido do `bcm-dev-host` (opcional).

### Estrutura alvo de pacotes

```
saged/src/main/java/br/gov/crateus/bcm/saged/
  package-info.java
  api/            DemandController, SpecialtyController, ReportController,
                  Telegram*Controller, DTOs
  application/    DemandService, ProtocolService, policies de visibilidade
  domain/         DemandStatus, regras puras
  infrastructure/ *Entity, *Repository
saged/src/main/resources/db/module-migration/
  V20260719__saged_schema.sql
```

Entidades JPA devem estender
`br.gov.crateus.bcm.devhost.persistence.SdkAuditableEntity` (Dev Host).

## 4. Keycloak DEV — roles e claims

Realm `bcm-sdk`, Admin Console em `http://localhost:8180`.

### Roles de realm a criar

| Realm role | Uso |
|---|---|
| `SAGED_ADMIN_GERAL` | Visão cidade inteira; gerencia técnicos; qualquer secretaria |
| `SAGED_ADMIN_SETOR` | Gestor da unidade (filtra por claim `org_unit_id`) |
| `SAGED_TECNICO_LIDER` | Fila do setor + relatórios |
| `SAGED_TECNICO` | Fila filtrada por especialidade(s) (`specialty_codes`) |

### Usuários de teste sugeridos

| Username | Roles |
|---|---|
| `saged-admin` | `SAGED_ADMIN_GERAL` |
| `saged-setor` | `SAGED_ADMIN_SETOR` |
| `saged-lider` | `SAGED_TECNICO_LIDER` |
| `saged-tecnico` | `SAGED_TECNICO` |

### Claims customizados a criar

Caminho: Client scopes → `roles` → Mappers → **User Attribute** → marcar
"Add to access token".

- `org_unit_id` (String)
- `specialty_codes` (String, ex.: `01` ou `01,02`)

| User | Attribute | Exemplo de valor |
|---|---|---|
| `saged-setor` | `org_unit_id` | UUID de unidade de teste |
| `saged-tecnico` | `org_unit_id` | mesmo UUID |
| `saged-tecnico` | `specialty_codes` | `01` ou `01,02` |

Uso no código:
```java
@PreAuthorize("hasRole('SAGED_ADMIN_GERAL')")
// hasRole = nome da realm role; o converter do Dev Host monta ROLE_*
```

> Importante: ler os claims no backend e aplicar no filtro da query. Nunca
> confiar apenas no front-end para restringir visibilidade.

## 5. DDL Flyway completo

Arquivo: `saged/src/main/resources/db/module-migration/V20260719__saged_schema.sql`

Pode-se adicionar colunas de domínio extras; não remover as colunas de
auditoria nem renomear as tabelas núcleo sem alinhamento com a Seplati.

```sql
CREATE SCHEMA IF NOT EXISTS saged;

-- Specialties (filas: Hardware=01, Redes=02, ...)
CREATE TABLE saged.specialties (
    id               UUID PRIMARY KEY,
    code             VARCHAR(32)  NOT NULL UNIQUE,
    name             VARCHAR(128) NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL,
    created_by       VARCHAR(255),
    updated_by       VARCHAR(255),
    org_id           UUID,
    source           VARCHAR(64),
    sensitivity      VARCHAR(32)  NOT NULL DEFAULT 'INTERNAL',
    lifecycle_status VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
    version          BIGINT       NOT NULL DEFAULT 0
);

CREATE TABLE saged.assets (
    id               UUID PRIMARY KEY,
    asset_tag        VARCHAR(128) NOT NULL UNIQUE,
    description      VARCHAR(512),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL,
    created_by       VARCHAR(255),
    updated_by       VARCHAR(255),
    org_id           UUID,
    source           VARCHAR(64),
    sensitivity      VARCHAR(32)  NOT NULL DEFAULT 'INTERNAL',
    lifecycle_status VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
    version          BIGINT       NOT NULL DEFAULT 0
);

-- user_id = UUID da plataforma (sem tabela users local)
CREATE TABLE saged.user_specialties (
    id               UUID PRIMARY KEY,
    user_id          UUID         NOT NULL,
    specialty_id     UUID         NOT NULL REFERENCES saged.specialties(id),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL,
    created_by       VARCHAR(255),
    updated_by       VARCHAR(255),
    org_id           UUID,
    source           VARCHAR(64),
    sensitivity      VARCHAR(32)  NOT NULL DEFAULT 'INTERNAL',
    lifecycle_status VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
    version          BIGINT       NOT NULL DEFAULT 0,
    UNIQUE (user_id, specialty_id)
);

-- status: A_FAZER | EM_ANDAMENTO | CONCLUIDO | INTERROMPIDO
-- department_id / requester_user_id = UUIDs lógicos (sem JOIN cross-schema)
CREATE TABLE saged.demands (
    id                     UUID PRIMARY KEY,
    protocol               VARCHAR(64)  NOT NULL UNIQUE,
    title                  VARCHAR(255) NOT NULL,
    description            TEXT         NOT NULL,
    status                 VARCHAR(64)  NOT NULL,
    requester_user_id      UUID         NOT NULL,
    department_id          UUID         NOT NULL,
    specialty_id           UUID         NOT NULL REFERENCES saged.specialties(id),
    asset_tag              VARCHAR(128),
    current_technical_note TEXT,
    assignee_user_id       UUID,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ  NOT NULL,
    created_by             VARCHAR(255),
    updated_by             VARCHAR(255),
    org_id                 UUID,
    source                 VARCHAR(64),
    sensitivity            VARCHAR(32)  NOT NULL DEFAULT 'INTERNAL',
    lifecycle_status       VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
    version                BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_saged_demands_department ON saged.demands (department_id);
CREATE INDEX idx_saged_demands_specialty  ON saged.demands (specialty_id);
CREATE INDEX idx_saged_demands_status     ON saged.demands (status);

CREATE TABLE saged.demand_history (
    id               UUID PRIMARY KEY,
    demand_id        UUID         NOT NULL REFERENCES saged.demands(id),
    action           VARCHAR(64)  NOT NULL,
    justification    TEXT,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL,
    created_by       VARCHAR(255),
    updated_by       VARCHAR(255),
    org_id           UUID,
    source           VARCHAR(64),
    sensitivity      VARCHAR(32)  NOT NULL DEFAULT 'INTERNAL',
    lifecycle_status VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
    version          BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_saged_demand_history_demand ON saged.demand_history (demand_id);

CREATE TABLE saged.telegram_demand_requesters (
    id               UUID PRIMARY KEY,
    telegram_chat_id VARCHAR(128) NOT NULL,
    phone_number     VARCHAR(64)  NOT NULL,
    display_name     VARCHAR(255),
    department_id    UUID         NOT NULL,
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL,
    created_by       VARCHAR(255),
    updated_by       VARCHAR(255),
    org_id           UUID,
    source           VARCHAR(64),
    sensitivity      VARCHAR(32)  NOT NULL DEFAULT 'INTERNAL',
    lifecycle_status VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
    version          BIGINT       NOT NULL DEFAULT 0
);

CREATE TABLE saged.telegram_requester_authorizations (
    id               UUID PRIMARY KEY,
    requester_id     UUID         NOT NULL REFERENCES saged.telegram_demand_requesters(id),
    department_id    UUID         NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL,
    created_by       VARCHAR(255),
    updated_by       VARCHAR(255),
    org_id           UUID,
    source           VARCHAR(64),
    sensitivity      VARCHAR(32)  NOT NULL DEFAULT 'INTERNAL',
    lifecycle_status VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
    version          BIGINT       NOT NULL DEFAULT 0,
    UNIQUE (requester_id, department_id)
);

CREATE TABLE saged.telegram_contacts (
    id               UUID PRIMARY KEY,
    telegram_user_id VARCHAR(128) NOT NULL UNIQUE,
    chat_id          VARCHAR(128) NOT NULL,
    phone_number     VARCHAR(64),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL,
    created_by       VARCHAR(255),
    updated_by       VARCHAR(255),
    org_id           UUID,
    source           VARCHAR(64),
    sensitivity      VARCHAR(32)  NOT NULL DEFAULT 'INTERNAL',
    lifecycle_status VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
    version          BIGINT       NOT NULL DEFAULT 0
);

CREATE TABLE saged.bot_processed_messages (
    id                  UUID PRIMARY KEY,
    provider            VARCHAR(32)  NOT NULL DEFAULT 'TELEGRAM',
    external_message_id VARCHAR(128) NOT NULL,
    processed_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL,
    created_by          VARCHAR(255),
    updated_by          VARCHAR(255),
    org_id              UUID,
    source              VARCHAR(64),
    sensitivity         VARCHAR(32)  NOT NULL DEFAULT 'INTERNAL',
    lifecycle_status    VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
    version             BIGINT       NOT NULL DEFAULT 0,
    UNIQUE (provider, external_message_id)
);
```

### Regras de dados (não negociáveis)

- `department_id` / `requester_user_id` / `user_id` são sempre **UUID da
  plataforma** — nunca usar nome de secretaria como fonte da verdade.
- Sem `JOIN` para `identity`/`organization`/`geography`.
- Soft-delete sempre via `lifecycle_status` (nunca `DELETE` físico).
- Protocolo de negócio no formato **`YYYY-DEP-TEC-NNNNN`**, gerado no
  service com transação/lock para não duplicar.

### Comandos de verificação após subir o Dev Host

```bash
docker exec -it bcm-sdk-postgres psql -U bcm_sdk -d bcm_sdk -c '\dn'
docker exec -it bcm-sdk-postgres psql -U bcm_sdk -d bcm_sdk -c '\dt saged.*'
```

## 6. Contrato mínimo da API web

Prefixo `/api/v1/saged`. Toda rota precisa de `@Operation` + `@Tag("saged")`.
Erros seguem Problem Details (404/409).

| Área | Paths | Auth |
|---|---|---|
| Smoke | `GET /hello` | autenticado (ou público em DEV) |
| Demandas | `POST /demands`, `GET /demands`, `PATCH /demands/{id}/status`, `PATCH /demands/{id}/view`, `GET /demands/{id}/history` | ver matriz UAT |
| Especialidades | `GET /specialties`, `POST /specialties` | criar: `SAGED_ADMIN_GERAL` |
| Assets | `GET /assets/{tag}` | autenticado |
| Relatórios | `GET/POST /demands/reports/...` | `ADMIN_GERAL`, `ADMIN_SETOR`, `TECNICO_LIDER` |
| Técnicos | CRUD vínculo user↔specialty | `SAGED_ADMIN_GERAL` |
| Telegram admin | `/telegram-demand-requesters` | `ADMIN_GERAL`, `ADMIN_SETOR` |
| Webhook | `POST /webhooks/telegram` | secret do bot (sem JWT de usuário) |

### Matriz UAT obrigatória (permissões por role)

| Ação | ADMIN_GERAL | ADMIN_SETOR | TECNICO_LIDER | TECNICO |
|---|---|---|---|---|
| Ver todas as secretarias | sim | não | não | não |
| Quadro da unidade | sim | sim | sim | só sua specialty |
| Criar demanda em outra unidade | sim | não | não | não |
| Relatórios de gestão | sim | sim | sim | não |
| Gerenciar técnicos | sim | não | não | não |
| Autorizar Telegram | sim | sim | não | não |

Fluxo do Kanban: `A_FAZER` → `EM_ANDAMENTO` → `CONCLUIDO` / `INTERROMPIDO`
(interrupção exige justificativa longa). Enviar `X-Correlation-Id` por
intenção de negócio (mesmo id em todas as chamadas HTTP do mesmo fluxo).

## 7. SPA (front-end)

| Item | DEV |
|---|---|
| Stack | React + TypeScript + Vite; Mantine; fonte Inter; header PVH |
| Auth | OIDC no realm `bcm-sdk`, client `bcm-sdk-public` |
| API | `http://localhost:8080` + `Authorization: Bearer ...` |
| Telas | login → unidade/fila → Kanban → novo chamado → relatórios (por role) |
| Header | `<pvh-header>`, assets em `/institutional/v1/` |

Não reimplementar login próprio com cookie/bcrypt.

## 8. Telegram (obrigatório na entrega)

1. Criar bot de teste no BotFather — token só em variável de ambiente local.
2. Variáveis de ambiente:
   ```bash
   export SAGED_TELEGRAM_BOT_TOKEN=...
   export SAGED_TELEGRAM_WEBHOOK_SECRET=...
   ```
3. Endpoints: `POST /api/v1/saged/webhooks/telegram` (validar secret);
   admin em `/api/v1/saged/telegram-demand-requesters`.
4. Tunnel local: `ngrok http 8080` → configurar webhook do bot para
   `https://<ngrok>/api/v1/saged/webhooks/telegram`.
5. Idempotência via tabela `bot_processed_messages` (não reprocessar o
   mesmo `update_id`/message id).

Fluxo mínimo de UAT do Telegram:
- Autorizar telefone/chat
- Abrir chamado pelo bot → aparece no Kanban com protocolo
- Mudança de status gera notificação (se estiver no escopo combinado)

## 9. Testes automatizados exigidos

- **Services**: criação de demanda, mudança de status, justificativa
  obrigatória em `INTERROMPIDO`, unicidade do protocolo.
- **Controllers**: MockMvc + JWT simulando cada role `SAGED_*` (200 vs 403).
- **Policy de visibilidade**: testes unitários com `department`/`specialty`
  diferentes.

```java
.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SAGED_TECNICO")))
```

## 10. Empacotamento da entrega final

- Código `br.gov.crateus.bcm.saged` (+ `build.gradle.kts`)
- Migrations Flyway (`V...__saged_schema.sql` + posteriores)
- SPA + README de build
- Nota técnica: versão do SDK (0.1.0), roles/claims usados, variáveis de
  ambiente do Telegram, UAT executado (web + Telegram), limitações conhecidas

A Seplati fica responsável por: portar o módulo para o monólito real,
rodar o Flyway no BDM de produção, criar as roles no Keycloak
institucional, configurar o bot oficial, e fazer o deploy em
staging/produção.

## 11. Checklist final (retomado da doc oficial)

- [ ] Zip 0.1.0 + compose + Dev Host funcionando
- [ ] Projeto Gradle `saged` criado; `GET /api/v1/saged/hello` responde
- [ ] Roles `SAGED_*` + usuários de teste + claims `org_unit_id` /
      `specialty_codes` configurados no Keycloak DEV
- [ ] Flyway criou o schema `saged` e todas as tabelas do DDL
- [ ] Matriz UAT web testada com os 4 logins de teste
- [ ] Telegram de teste validado: webhook + chamado aparece no Kanban
- [ ] Testes automatizados de autorização escritos e passando
- [ ] Pacote final de entrega sem nenhum secret de produção
