# Arquitetura — OBM Backend

## Estrutura do projeto

```
OMD Backend/
├── server.js              # Entrypoint: HTTP + WS + middleware
├── database.js            # Pool PostgreSQL, initDb, colunas dinâmicas
├── auth.js                # JWT + plugin key middlewares
├── ws.js                  # WebSocket broadcast
├── discord.js             # Webhooks Discord (logs admin)
├── package.json
├── public/                # Dashboard estática
│   ├── index.html
│   └── dashboard.js
├── routes/                # Rotas modulares Express
│   ├── index.js           # Agregador
│   ├── auth.js            # POST /login
│   ├── players.js         # playerJoin, players, player/:uuid
│   ├── economy.js         # money/add
│   ├── logs.js            # log
│   ├── commands.js        # command, commands
│   ├── stats.js           # stats, tops, sync
│   ├── debug.js           # debug/columns
│   └── validation.js      # Helpers UUID, erros
├── reference/             # Código legado (NÃO usado em runtime)
└── docs/documentacao/backend/
```

## server.js — responsabilidades

```
┌─────────────────────────────────────────┐
│  express.json()                         │
│  express.static('public')               │
│  GET /health  (ping DB)                 │
│  app.use(routes)  ← todas as APIs       │
└─────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────┐
│  http.createServer(app)                 │
│  initWebSocket(server)  →  /ws          │
│  initDb() → migrations + colunas        │
│  server.listen(PORT)                    │
└─────────────────────────────────────────┘
```

**Regra:** `server.js` não deve definir rotas de negócio — apenas health + routes.

## routes/index.js — ordem de montagem

```javascript
router.use(require('./debug'));
router.use(require('./auth'));
router.use(require('./stats'));
router.use(require('./players'));
router.use(require('./economy'));
router.use(require('./logs'));
router.use(require('./commands'));
```

Todas montadas na **raiz** (`/`) — compatível com plugin existente.

## database.js — camadas

| Função | Propósito |
|--------|-----------|
| `initDb()` | CREATE TABLE IF NOT EXISTS + ALTER migrations |
| `resolvePlayerColumns()` | Deteta `coins`/`balance`/`money` |
| `col('coins')` | Nome real da coluna (SQL seguro) |
| `sqlPlayerSelect()` | SELECT padronizado com aliases |
| `getPlayerColumns()` | Debug/monitoring |

## auth.js — middlewares

| Middleware | Uso |
|------------|-----|
| `authMiddleware` | Só JWT (dashboard) |
| `pluginAuth` | Só plugin key (exportado, não usado nas routes) |
| `pluginOrAuthMiddleware` | Plugin key **ou** JWT |

## ws.js — eventos

`broadcast({ type: 'update' })` emitido em:

- `POST /playerJoin`
- `POST /money/add`
- `POST /stats/sync`

Dashboard reconecta e recarrega dados ao receber.

## discord.js

Chamado em `POST /log` → embed Discord categorizado (admin/economy/punish).

## Fluxo de um request

```
Cliente → Express middleware → Route handler
         → auth (se aplicável)
         → validation.js
         → pool.query (database.js)
         → broadcast (opcional)
         → res.json / sendStatus
```
