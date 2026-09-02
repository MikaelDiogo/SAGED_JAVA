# SAGED — SPA (Frontend)

SPA do módulo SAGED: React + TypeScript + Vite + Mantine, autenticação OIDC PKCE
no realm `bcm-sdk` (client público `bcm-sdk-public`) e Kanban com drag-and-drop.

## Stack

- **React 19 + TypeScript + Vite**
- **Mantine 9** (`@mantine/core`, `form`, `hooks`, `notifications`, `charts`)
- **Kanban**: `@hello-pangea/dnd`
- **Auth**: `oidc-client-ts` + `react-oidc-context` (Authorization Code + PKCE)
- **HTTP**: `axios` com `Authorization: Bearer <JWT>`

## Pré-requisitos

- Node 20+
- Dev Host rodando em `http://localhost:8080` (`./gradlew :bcm-dev-host:bootRun`)
- Keycloak realm `bcm-sdk` acessível (client `bcm-sdk-public`, PKCE)

## Configuração

Copie `.env.example` para `.env` e ajuste se necessário:

```
VITE_API_URL=http://localhost:8080/api/v1/saged
VITE_KEYCLOAK_URL=http://localhost:8180/realms/bcm-sdk
VITE_KEYCLOAK_CLIENT_ID=bcm-sdk-public
```

## Build e execução

```bash
npm install       # instala dependências
npm run dev       # ambiente de desenvolvimento (Vite, HMR)
npm run build     # build de produção (tsc -b && vite build) → dist/
npm run preview   # serve o build de produção localmente
npm run lint      # eslint
```

## Telas

Login (OIDC PKCE) → seleção de unidade/fila → Kanban (arrastar entre colunas
`TODO / IN_PROGRESS / DONE / INTERRUPTED`) → novo chamado → relatórios (por role).

Os identificadores de status são em inglês na API; a SPA os traduz para PT-BR.

## Unidades (rótulos)

- **Produção**: `GET /api/v1/organization/units` (plataforma).
- **DEV**: fixture local `public/dev-org-units.json` (o módulo `saged` não lista secretarias).

## PVH (identidade visual)

Header institucional `<pvh-header>` e tokens/logo servidos a partir de
`https://api.pontodatec.com.br/institutional/v1/`.
