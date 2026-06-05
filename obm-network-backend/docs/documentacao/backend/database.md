# Base de Dados — PostgreSQL

## Conexão

- **Driver:** `pg` Pool
- **Env:** `DATABASE_URL`
- **SSL:** automático em produção (não localhost)
- **Pool:** max 20 conexões, idle 30s

## Tabela `players`

| Coluna | Tipo | Default | Origem | Notas |
|--------|------|---------|--------|-------|
| `id` | SERIAL | auto | CREATE TABLE | PK interno |
| `uuid` | TEXT UNIQUE | — | CREATE TABLE | Identificador Minecraft |
| `name` | TEXT | — | CREATE TABLE | Nome display |
| `world` | TEXT | — | CREATE TABLE | Mundo atual |
| `coins` | INT | 0 | CREATE / ALTER | Pode ser `balance`/`money` na DB real |
| `emeralds` | INT | 0 | CREATE / ALTER | |
| `rank` | VARCHAR(50) | bronze | CREATE + ALTER | Migration recente |
| `created_at` | TIMESTAMP | NOW() | CREATE TABLE | Pode não existir em DBs legadas |
| `kills` | INT | 0 | ALTER migration | Leaderboard futuro |
| `playtime` | INT | 0 | ALTER migration | Segundos |
| `updated_at` | TIMESTAMPTZ | NOW() | ALTER migration | Última sync |

### Deteção dinâmica de colunas

`resolvePlayerColumns()` mapeia:

- **coins:** `coins` → `balance` → `money` (ou `COINS_COLUMN` env)
- **emeralds:** `emeralds` → `emerald` (ou `EMERALDS_COLUMN` env)

Queries usam `col('coins')` — nunca hardcode assumido.

### Índices

Nenhum índice em `players` actualmente (só em `commands`).

---

## Tabela `logs`

| Coluna | Tipo | Descrição |
|--------|------|-----------|
| `id` | SERIAL PK | |
| `staff` | TEXT | Quem executou |
| `action` | TEXT | Tipo de acção |
| `target` | TEXT | Alvo |
| `value` | TEXT | Detalhe extra |
| `created_at` | TIMESTAMP | Quando |

**Nota:** Reference usa `audit_logs` — backend actual usa `logs`.

---

## Tabela `commands`

| Coluna | Tipo | Descrição |
|--------|------|-----------|
| `id` | SERIAL PK | |
| `command` | TEXT NOT NULL | Comando Minecraft raw |
| `executed` | BOOLEAN | false = pendente |
| `created_at` | TIMESTAMP | |

**Índice:** `idx_commands_pending` — partial index WHERE `executed = false`

---

## Relações entre dados

```
                    ┌─────────────┐
   playerJoin ─────►│   players   │◄──── stats/sync
   money/add  ─────►│  (uuid PK)  │
   GET /player ─────│             │
                    └──────┬──────┘
                           │
              ┌────────────┼────────────┐
              ▼            ▼            ▼
        GET /players  GET /top/*   stats/global
        (dashboard)   (dashboard)  (dashboard)

   POST /log ──────► logs ──────► discord.js (webhook)

   POST /command ──► commands ◄── GET /commands (plugin poll)
                         │
                         └── executed=true após leitura
```

## Fluxo de dados plugin ↔ DB

1. **Join** → cria/atualiza row em `players` (name, world)
2. **Money** → UPDATE delta em coluna coins
3. **Stats sync** → UPSERT valores absolutos (coins, kills, playtime)
4. **Log** → INSERT em `logs` (independente de players)
5. **Command** → INSERT fila → plugin consome via GET

## Migrations automáticas (initDb)

Executadas em cada arranque:

```sql
CREATE TABLE IF NOT EXISTS ...
ALTER TABLE players ADD COLUMN IF NOT EXISTS kills ...
ALTER TABLE players ADD COLUMN IF NOT EXISTS playtime ...
ALTER TABLE players ADD COLUMN IF NOT EXISTS updated_at ...
ALTER TABLE players ADD COLUMN IF NOT EXISTS rank ...
```

**Limitação:** `CREATE TABLE IF NOT EXISTS` não altera tabelas já existentes com schema diferente.
