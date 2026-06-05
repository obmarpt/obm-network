# Endpoints — Referência completa

Base URL: `https://obm-network-backend.onrender.com` (ou `http://localhost:3000`)

Legenda auth:
- **Público** — sem auth
- **JWT** — `Authorization: Bearer <token>`
- **Plugin** — `X-Plugin-Key: <PLUGIN_API_KEY>`
- **Plugin∨JWT** — um dos dois
- **Dev** — se `PLUGIN_API_KEY` não definida, aceita sem header

---

## Sistema

### GET /health

| | |
|---|---|
| **Auth** | Público |
| **Descrição** | Health check + ping PostgreSQL |
| **Resposta 200** | `{ "ok": true, "service": "obm-backend" }` |
| **Resposta 503** | `{ "ok": false, "error": "internal error" }` |

---

## Autenticação

### POST /login

| | |
|---|---|
| **Auth** | Público |
| **Body** | `{ "username": "admin", "password": "..." }` |
| **Resposta 200** | `{ "token": "...", "expiresIn": "2h" }` |
| **Erros** | 400 validação · 401 credenciais · 503 auth não configurado |

---

## Players

### POST /playerJoin

| | |
|---|---|
| **Auth** | Público |
| **Body** | `{ "uuid": "...", "name": "Steve", "world": "world" }` |
| **Resposta** | `200` (vazio) |
| **Erros** | 400 uuid/name/world · 500 internal |

Upsert: atualiza `name`, `world`, `updated_at`. Emite WebSocket `update`.

### GET /players

| | |
|---|---|
| **Auth** | JWT |
| **Resposta 200** | Array de players com `id, uuid, name, world, coins, emeralds, rank, created_at` |

### GET /player/:uuid

| | |
|---|---|
| **Auth** | Público |
| **Resposta 200** | `{ uuid, name, coins, balance, money, emeralds, rank }` |
| **Erros** | 400 uuid inválido · 404 não encontrado |

Aliases `balance`/`money` = `coins` (compat plugin).

---

## Economia

### POST /money/add

| | |
|---|---|
| **Auth** | Plugin∨JWT |
| **Body** | `{ "uuid": "...", "amount": 100 }` — negativo permitido |
| **Resposta** | `200` (vazio) |
| **Erros** | 400 · 403 key inválida · 404 player · 500 |

Delta: `coins = coins + amount`. Emite WebSocket `update`.

---

## Logs

### POST /log

| | |
|---|---|
| **Auth** | Plugin∨JWT |
| **Body** | `{ "staff": "Admin", "action": "BAN", "target": "Steve", "value": "hack" }` |
| **Resposta** | `200` (vazio) |
| **Side effect** | INSERT `logs` + Discord webhook |

---

## Comandos remotos

### POST /command

| | |
|---|---|
| **Auth** | Plugin∨JWT |
| **Body** | `{ "command": "kick Steve" }` |
| **Resposta** | `200` (vazio) |

### GET /commands

| | |
|---|---|
| **Auth** | Plugin∨JWT (plugin faz polling) |
| **Resposta 200** | `[{ "id", "command", "created_at" }, ...]` |

Atómico: devolve pendentes e marca `executed = true`.

---

## Stats & Leaderboards

### GET /stats/global

| | |
|---|---|
| **Auth** | JWT |
| **Resposta 200** | `{ totalPlayers, totalCoins, avgCoins }` |

### POST /stats/sync

| | |
|---|---|
| **Auth** | Plugin∨JWT |
| **Body** | `{ uuid, name, coins, emeralds, kills, playtime }` |
| **Resposta 200** | `{ "ok": true, "uuid": "..." }` |

Upsert total (não delta). Emite WebSocket `update`.

### GET /top/coins

| | |
|---|---|
| **Auth** | JWT |
| **Resposta 200** | `[{ name, uuid, coins }, ...]` (max 10) |

### GET /top/emeralds

| | |
|---|---|
| **Auth** | JWT |
| **Resposta 200** | `[{ name, uuid, emeralds }, ...]` (max 10) |

---

## Debug

### GET /debug/columns

| | |
|---|---|
| **Auth** | JWT |
| **Resposta 200** | `{ columns, mapped, hint }` |

---

## Estáticos & WebSocket

| Path | Tipo | Auth |
|------|------|------|
| `/` | Dashboard HTML | Público |
| `/dashboard.js` | JS | Público |
| `/ws` | WebSocket | Público (sem auth) |

---

## Códigos HTTP usados

| Código | Significado |
|--------|-------------|
| 200 | Sucesso |
| 204 | Sucesso sem body (`sendStatus`) |
| 400 | Input inválido |
| 401 | JWT em falta |
| 403 | Plugin key inválida ou JWT expirado |
| 404 | Recurso não encontrado |
| 500 | Erro interno `{ "error": "internal error" }` |
| 503 | Auth/health não configurado |
