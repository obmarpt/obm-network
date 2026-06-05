# OBM Network Backend

API Node.js + PostgreSQL para MineSpace (Render).

## Endpoints de leaderboard

| Método | Path | Descrição |
|--------|------|-----------|
| GET | `/top/coins` | Top 10 por coins |
| GET | `/top/emeralds` | Top 10 por emeralds |
| GET | `/top/kills` | Top 10 por kills |
| GET | `/top/playtime` | Top 10 por playtime (segundos) |

Query opcional: `?limit=10` (máx. 50)

Resposta:

```json
{
  "stat": "coins",
  "entries": [
    { "rank": 1, "uuid": "...", "name": "Player", "value": 120000 }
  ]
}
```

## Setup

```bash
cp .env.example .env
# editar DATABASE_URL
psql $DATABASE_URL -f schema.sql
npm install
npm start
```

## Deploy Render

- **Build:** `npm install`
- **Start:** `npm start`
- Variável: `DATABASE_URL` (PostgreSQL)

> Se já tens rotas `/player`, `/money/add` noutro ficheiro no deploy, mantém-as e importa só `routes/top.js`.
