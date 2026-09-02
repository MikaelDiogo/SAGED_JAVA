# Nota Técnica — Módulo SAGED

SDK: `bcm-sdk-api` **0.1.0** · Pacote: `br.gov.crateus.bcm.saged` · Schema Flyway: `saged`

Este documento cobre roles, claims JWT, variáveis de ambiente do Telegram, escopo de UAT
(web + Telegram) e limitações conhecidas, conforme exigido para entrega do módulo.

---

## 1. Roles (Keycloak realm `bcm-sdk`)

O módulo consome as roles de plataforma abaixo. Todas as rotas usam `@PreAuthorize` com
`hasAnyRole('SAGED_...')`. Não há `SecurityFilterChain` no módulo — a segurança é da plataforma.

| Role | Escopo |
|---|---|
| `SAGED_ADMIN_GERAL` | Vê todas as secretarias; cria em qualquer unidade; gerencia técnicos; autoriza Telegram |
| `SAGED_ADMIN_SETOR` | Restrito à própria unidade (`org_unit_id`); relatórios de gestão; autoriza Telegram |
| `SAGED_TECNICO_LIDER` | Restrito à própria unidade; relatórios de gestão |
| `SAGED_TECNICO` | Restrito à própria unidade **e** às suas especialidades (`specialty_codes`) |

## 2. Claims JWT

| Claim | Tipo | Uso |
|---|---|---|
| `sub` | UUID (String) | Identidade do usuário; `requesterUserId`/`assignee_user_id` derivam daqui |
| `preferred_username` | String | Nome exibido na UI (não é persistido no schema) |
| `org_unit_id` | **String** (UUID) | Unidade do usuário. Obrigatório para `ADMIN_SETOR`/`TECNICO_LIDER`/`TECNICO`; ausência → **403** |
| `specialty_codes` | **String** (`"01"` ou `"01,02"`) | Especialidades do técnico. Parser aceita String (`split(",")`) e List. Vazio → não vê nada (**nunca** "ver tudo") |

## 3. Variáveis de ambiente (Telegram)

Apenas duas variáveis. Nenhum token em código ou no pacote.

| Variável | Descrição |
|---|---|
| `SAGED_TELEGRAM_BOT_TOKEN` | Token do bot (BotFather) |
| `SAGED_TELEGRAM_WEBHOOK_SECRET` | Secret do webhook. **Obrigatório**: vazio → webhook responde `401` sempre |

Webhook inbound único: `POST /api/v1/saged/webhooks/telegram`
(secret via header `X-Telegram-Bot-Api-Secret-Token`, comparação timing-safe `MessageDigest.isEqual`).
Idempotência por `update_id` do Telegram na tabela `saged.bot_processed_messages`.

## 4. Divergências de contrato (documentadas e aceitas)

| Item | Contrato sugerido | Implementado | Observação |
|---|---|---|---|
| Status enum | `A_FAZER/EM_ANDAMENTO/...` | `TODO/IN_PROGRESS/DONE/INTERRUPTED` | Identificadores em inglês no código/DB; SPA e Telegram traduzem para PT |
| Códigos de especialidade | `01` Hardware, `02` Redes | `MANUT` (Manutenção), `INTERNET` (Internet) | Códigos estáveis; os mesmos valores devem ir no claim `specialty_codes` |
| Formato de protocolo | `YYYY-DEP-TEC-NNNNN` | `YYYY-{unidade}-{especialidade}-NNNNN` (ex.: `2026-1234-MANUT-00001`) | Estrutura equivalente (ano + unidade + especialidade + sequência), única, gerada sob lock pessimista (`findWithLockByCode`) |

## 5. Escopo de UAT executado

**Web (MockMvc + JWT `ROLE_SAGED_*`)** — matriz de visibilidade validada:
- `TECNICO_LIDER` não enxerga demanda de outra unidade (list, get-by-id, history, viewers)
- `TECNICO` filtrado por `specialty_codes`; sem specialty → não vê
- `ADMIN_SETOR` sem `org_unit_id` → 403
- Relatórios: 200 para as 3 roles de gestão, **403** para `SAGED_TECNICO`
- Create: `requesterUserId = jwt.sub`; `departmentId` do claim exceto para `ADMIN_GERAL`

**Telegram**
- Webhook sem secret → 401; com secret → chamado do bot aparece no Kanban
- `update_id` duplicado não reprocessa
- Mini-app valida `initData` via HMAC-SHA256; `chatId` do cliente é ignorado
- Aprovação de requester por `ADMIN_SETOR` restrita à própria unidade

## 6. Limitações conhecidas

- **Sessão do bot Telegram** em `ConcurrentHashMap` (com persistência de link codes no banco):
  reinício do Dev Host pode perder o fluxo conversacional em andamento. Aceitável em DEV.
- **Catálogo de unidades**: o módulo não lista secretarias. Em produção a SPA consome
  `GET /api/v1/organization/units`; em DEV usa fixture no front (`dev-org-units.json`).
- **Nomes de pessoas/órgãos** não são persistidos no schema `saged` — apenas UUIDs
  (`department_id`, `requester_user_id`, `assignee_user_id`); rótulos vêm do JWT/IdP.

## 7. SPA

React + TypeScript + Vite + Mantine + OIDC PKCE (realm `bcm-sdk`, client `bcm-sdk-public`).
Pasta separada `SAGED_FrontEnd/` com README de build (`npm install` / `npm run build`).
API base `http://localhost:8080` com `Authorization: Bearer <JWT>`.
