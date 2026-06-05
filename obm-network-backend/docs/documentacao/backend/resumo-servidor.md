# Resumo do Servidor — OBM Backend

## O que é

API Node.js (Express) + PostgreSQL que liga o servidor Minecraft ao painel admin web. Corre num único processo HTTP com WebSocket integrado.

## Fluxo principal

1. **Plugin Minecraft** envia eventos (join, coins, logs, comandos) via HTTP.
2. **PostgreSQL** guarda players, logs e comandos pendentes.
3. **Dashboard** (`public/`) autentica com JWT e consulta/age sobre os dados.
4. **WebSocket** (`/ws`) notifica a dashboard em tempo real quando há alterações.

## Autenticação dual

| Actor | Método |
|-------|--------|
| Dashboard admin | `POST /login` → JWT (`Authorization: Bearer`) |
| Plugin Minecraft | Header `X-Plugin-Key: <PLUGIN_API_KEY>` |
| Modo dev | Sem `PLUGIN_API_KEY` → endpoints do plugin abertos |

Rotas sensíveis usam `pluginOrAuthMiddleware`: aceita **plugin key OU JWT**.

## Integração plugin

Endpoints críticos para o plugin (paths na raiz, não `/api`):

- `POST /playerJoin` — registo/atualização ao entrar (público)
- `GET /player/:uuid` — saldo e rank (público)
- `POST /money/add` — delta de coins (protegido)
- `POST /log` — audit staff (protegido)
- `POST /command` / `GET /commands` — fila remota (protegido)
- `POST /stats/sync` — sync completa kills/playtime (protegido)

## Colunas dinâmicas

`database.js` deteta automaticamente se a DB usa `coins`, `balance` ou `money` — compatível com bases legadas.

## Deploy

- **Render:** `npm start` → `node server.js`
- **Env obrigatórias:** `DATABASE_URL`, `JWT_SECRET`, `ADMIN_USER`, `ADMIN_PASS`
- **Env recomendada:** `PLUGIN_API_KEY`, `DISCORD_WEBHOOK_URL`

## Stack

Express 4 · pg 8 · jsonwebtoken 9 · ws 8 · PostgreSQL
