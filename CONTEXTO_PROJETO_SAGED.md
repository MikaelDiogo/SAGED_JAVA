# Contexto do Projeto — Módulo SAGED (BCM Prefeitura de Crateús)

Este documento é o **contexto mestre** do projeto. Ele existe para que qualquer
assistente de IA (Claude Code, etc.) que trabalhe neste repositório entenda,
sem perguntas, o que é este projeto, onde ele se encaixa, o que pode e o que
**não pode** ser feito, e por onde começar.

> Leia este arquivo inteiro antes de gerar qualquer código.

---

## 1. O que é o BCM (contexto da plataforma)

A Prefeitura de Crateús está migrando seus sistemas para uma plataforma única
chamada **BCM (Backend Central Municipal)**:

- O BCM é um **monólito modular** (Spring Modulith) que é o **único** sistema
  autorizado a falar diretamente com o banco de dados central, o **BDM**
  (PostgreSQL/PostGIS).
- Sistemas externos podem se conectar de duas formas:
  - **Satélite**: só consome a API HTTP do BCM (front-end puro).
  - **Módulo**: código Java que roda **dentro** do BCM, usando o SDK oficial.
- **Este projeto (SAGED) é um Módulo.**

Referência oficial da plataforma (não editável por nós, é documentação da
Seplati): `https://api.pontodatec.com.br/institutional/v1/docs/index.html`

### Regras de governança que NUNCA podem ser quebradas

- Não temos acesso ao monorepo `CDM-Prefeitura` (fechado, nem com NDA).
- Nunca criar autenticação própria (login/senha) dentro do módulo — tudo passa
  pelo Keycloak/JWT.
- Nunca fazer JOIN direto com schemas de outros domínios (`identity`,
  `organization`, `geography`) — só via API.
- Nunca versionar segredos de produção (`.env` real, realm real do Keycloak,
  dump de banco real).
- O que produzimos aqui **não vira PR no monorepo da prefeitura**. No fim,
  entregamos um pacote (código do módulo + migrations + nota técnica) por
  canal seguro combinado com o coordenador (Seplati).

---

## 2. O que é o SAGED (este projeto)

**SAGED** = Sistema de Acompanhamento e Gestão de Demandas (nome de trabalho).
É o módulo de gestão de chamados/demandas de TI da secretaria, com:

- Quadro **Kanban** de demandas (`A_FAZER → EM_ANDAMENTO → CONCLUIDO/INTERROMPIDO`)
- Histórico/auditoria de cada demanda (`demand_history`)
- Protocolo único por chamado no formato `YYYY-DEP-TEC-NNNNN`
- Bot de notificações no **Telegram** (webhook)
- Controle de acesso por papel (role) dentro do próprio Keycloak da
  plataforma

### Convenções obrigatórias do módulo

| Item | Valor |
|---|---|
| Module id | `saged` |
| Pacote Java | `br.gov.crateus.bcm.saged` |
| Schema do banco | `saged` |
| Prefixo de rotas API | `/api/v1/saged/...` |
| Camadas obrigatórias | `api/`, `application/`, `domain/`, `infrastructure/` |

### Roles do módulo (a criar no Keycloak)

- `SAGED_ADMIN_GERAL`
- `SAGED_ADMIN_SETOR`
- `SAGED_TECNICO_LIDER`
- `SAGED_TECNICO`

(A matriz completa de permissões por role — quem vê o quê — está na doc
oficial, seção do módulo SAGED. Reproduzir aqui ao criar os controllers.)

### Toda tabela de negócio deve ter (auditoria obrigatória / LGPD)

```
id, created_at, updated_at, created_by, updated_by,
org_id, source,
sensitivity        (PUBLIC | INTERNAL | SENSITIVE | CONFIDENTIAL)
lifecycle_status   (ACTIVE | INACTIVE | ARCHIVED | DELETED)  -- soft delete, nunca DELETE físico
version            (lock otimista)
```

O SDK já expõe essa interface pronta em
`br.gov.crateus.bcm.sdk.persistence.AuditableRecord` — toda entidade JPA do
módulo deve implementá-la (via a classe base do Dev Host,
`SdkAuditableEntity`).

---

## 3. Estrutura deste repositório

```
.
├── CONTEXTO_PROJETO_SAGED.md      ← este arquivo
├── docs/
│   └── bcm-institutional-docs/    ← cópia/print da doc oficial da plataforma (HTML salvo)
├── sdk/                           ← conteúdo do bcm-module-sdk-0.1.0.zip (fornecido pela Seplati)
│   ├── bcm-sdk-api/                → contratos Java (AuditableRecord, OutboxRecorder, @BcmBusinessModule)
│   ├── bcm-dev-host/               → servidor local que simula o BCM (JWT + Flyway + outbox)
│   ├── module-skeleton/            → exemplo de módulo, ponto de partida
│   ├── docker-compose.yml          → Postgres (:5433) + Keycloak (:8180)
│   ├── docker/keycloak/realm-bcm-sdk.json  → realm pré-configurado (roles ADMIN/USER genéricas)
│   ├── settings.gradle.kts
│   ├── gradlew / gradlew.bat
│   └── README.md
└── saged/                          ← MÓDULO EM DESENVOLVIMENTO (cópia renomeada do module-skeleton)
    ├── build.gradle.kts
    └── src/main/
        ├── java/br/gov/crateus/bcm/saged/
        │   ├── package-info.java
        │   ├── api/            (controllers REST)
        │   ├── application/    (services, casos de uso)
        │   ├── domain/         (entidades, regras de negócio)
        │   └── infrastructure/ (repositories, integrações externas, Telegram)
        └── resources/db/module-migration/
            └── V20260719__saged_schema.sql
```

> **Importante**: o zip do SDK e o HTML da doc institucional devem ser colocados
> nas pastas `sdk/` e `docs/` respectivamente antes de abrir o projeto no
> IntelliJ, para que o Claude Code tenha o contexto completo em disco.

---

## 4. Stack técnica

- **Java 21** + **Spring Boot 3** (Spring Modulith no BCM real; aqui rodamos
  isolado via Dev Host)
- **PostgreSQL 16** + PostGIS (schema `saged`)
- **Flyway** para migrations (`src/main/resources/db/module-migration/`)
- **Keycloak 26.2** para autenticação/autorização (JWT/OAuth2/OIDC)
- **RabbitMQ + Outbox pattern** para eventos entre módulos (via
  `OutboxRecorder`, gravado na mesma transação do negócio)
- **Gradle** (usar sempre `./gradlew`, nunca instalar Gradle manualmente)
- Front-end (se houver SPA do SAGED): **React + TypeScript + Vite + Mantine**,
  fonte Inter, header institucional obrigatório via componente `<pvh-header>`

---

## 5. Como rodar o ambiente local (Dev Host)

### Pré-requisitos a instalar

- **JDK 21** (Temurin/Eclipse Adoptium recomendado)
- **Docker** + **Docker Compose**
- **IntelliJ IDEA** (Ultimate ou Community) com plugin **Claude Code**
- Não precisa instalar Gradle: o projeto já traz `gradlew`

### Passo a passo

```bash
# 1. Subir infraestrutura local (Postgres + Keycloak)
cd sdk
docker compose up -d
docker compose ps   # confirmar que os dois containers estão healthy

# 2. Rodar o Dev Host (simulador do BCM)
cd ..
./sdk/gradlew -p sdk :bcm-dev-host:bootRun
```

- API do Dev Host: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Keycloak Admin Console: `http://localhost:8180` (login `admin`/`admin`)
- Realm: `bcm-sdk` (já importado automaticamente via `--import-realm`)
- Usuários de teste prontos: `dev-admin`/`dev-admin` (role `ADMIN`),
  `dev-user`/`dev-user` (role `USER`)

### Smoke test (confirmar que subiu certo)

```
GET http://localhost:8080/api/v1/system/ping
GET http://localhost:8080/api/v1/example/hello
```

---

## 6. O que falta configurar manualmente (não vem pronto no zip)

O realm `bcm-sdk` importado só traz roles genéricas (`ADMIN`, `USER`). Para o
SAGED, é preciso criar manualmente no Admin Console (`localhost:8180`):

1. **4 roles de realm**: `SAGED_ADMIN_GERAL`, `SAGED_ADMIN_SETOR`,
   `SAGED_TECNICO_LIDER`, `SAGED_TECNICO`
2. **2 claims customizados** (client scope → mapper "User Attribute" →
   adicionar ao access token): `org_unit_id`, `specialty_codes`
3. **4 usuários de teste**, um por role, para validar a matriz de permissões
   (200 vs 403) antes de entregar

---

## 7. Passo a passo para começar o módulo SAGED

### Passo 1 — Copiar o skeleton

```bash
cp -R sdk/module-skeleton saged
```

Editar `sdk/settings.gradle.kts` para incluir o novo módulo:
```kotlin
include("saged")
```

Em `sdk/bcm-dev-host/build.gradle.kts`, trocar a dependência de exemplo por:
```kotlin
implementation(project(":saged"))
```

### Passo 2 — Renomear pacote

De `br.gov.crateus.bcm.example` para `br.gov.crateus.bcm.saged` em todos os
arquivos (`package-info.java`, controllers, etc.)

Marcar o módulo com a anotação do SDK:
```java
@BcmBusinessModule(id = "saged", displayName = "SAGED")
```

### Passo 3 — Migration inicial

Renomear e adaptar `V20260720__saged_schema.sql` para
`V20260719__saged_schema.sql`, trocando `example` por `saged` e criando as
tabelas reais do domínio (`specialties`, `assets`, `user_specialties`,
`demands`, `demand_history`, tabelas de Telegram, `bot_processed_messages`) —
DDL completo está na doc oficial da plataforma, seção "Módulo SAGED".

Toda tabela deve seguir o padrão de auditoria da seção 2 acima.

### Passo 4 — Primeiro controller de negócio

Substituir `ExampleController` por um primeiro endpoint real do domínio,
seguindo o padrão:
```java
@RestController
@RequestMapping("/api/v1/saged")
@Tag(name = "saged")
public class SagedDemandController { ... }
```

Usar `@PreAuthorize("hasRole('SAGED_TECNICO')")` (ou a role correspondente)
em cada endpoint, nunca deixar endpoint de negócio sem controle de acesso.

### Passo 5 — Ordem recomendada de implementação

1. Skeleton copiado e rodando (`/api/v1/saged/hello` responde) ✅ marco 1
2. Migrations Flyway completas (todas as tabelas do domínio)
3. Roles + claims + usuários de teste no Keycloak
4. Camada `domain/` — entidades e regras (protocolo único, transições de
   status do Kanban)
5. Camada `application/` — services/casos de uso
6. Camada `api/` — controllers + `@PreAuthorize` por role
7. Camada `infrastructure/` — repositories JPA, integração Telegram (webhook
   `POST /api/v1/saged/webhooks/telegram`, validando secret + idempotência via
   `bot_processed_messages`)
8. Eventos via `OutboxRecorder` (gravar outbox na mesma transação do negócio)
9. Testes automatizados de autorização (matriz de roles: quem recebe 200,
   quem recebe 403)

---

## 8. Checklist final antes de "entregar" para a Seplati

- [ ] Projeto compila e sobe limpo no Dev Host
- [ ] Migrations rodam do zero sem erro, todas as tabelas com colunas de
      auditoria completas
- [ ] Matriz de autorização testada por role (200 vs 403) para cada endpoint
- [ ] `X-Correlation-Id` propagado corretamente em cada fluxo de negócio
- [ ] Eventos gravados no Outbox na mesma transação do negócio que os gerou
- [ ] Nenhum secret de produção commitado (verificar `.gitignore`)
- [ ] Nota técnica escrita: versão do SDK usada, roles necessárias, variáveis
      de ambiente, testes realizados, limitações conhecidas
- [ ] Pacote final = pasta `saged/` + migrations + SPA (se houver) + nota
      técnica — **não** é PR no monorepo, é entrega por canal seguro

---

## 9. Regras de versionamento (Git)

Este projeto **não é** versionado no monorepo da prefeitura. Ele vive em
repositório próprio (privado), só com o código do módulo — o Dev Host e o
zip do SDK são apoio de desenvolvimento local, não fazem parte da entrega
final.

`.gitignore` sugerido:
```
build/
.gradle/
*.class
out/
.idea/
docker/keycloak/realm-bcm-sdk.json.bak
```

Nunca commitar: `.env` reais, tokens/segredos do Keycloak de produção, dump
de banco real.

---

## 10. O que este assistente (Claude Code) deve e não deve fazer aqui

**Pode:**
- Gerar código dentro de `saged/` seguindo as convenções acima
- Criar/editar migrations Flyway do schema `saged`
- Sugerir testes de autorização e estrutura de camadas
- Ajudar a escrever a nota técnica de entrega

**Não pode:**
- Sugerir clonar, acessar ou espelhar o monorepo `CDM-Prefeitura`
- Criar autenticação própria (login/senha) dentro do módulo
- Fazer o módulo acessar diretamente tabelas de `identity`/`organization`/
  `geography` (só via API/eventos)
- Commitar ou sugerir commitar segredos reais de produção
