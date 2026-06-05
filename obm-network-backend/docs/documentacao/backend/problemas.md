# Problemas identificados — Análise automática

> Análise baseada no código em `server.js`, `routes/*`, `database.js`, `auth.js`, `ws.js`, `public/`.
> Data: Junho 2026

---

## 🔴 Críticos

### 1. `GET /players` — ORDER BY `created_at` pode falhar

**Ficheiro:** `routes/players.js:38`

```sql
ORDER BY created_at DESC
```

**Problema:** Bases legadas (ex: `reference/schema.sql`) não têm `created_at` — só `updated_at`.

**Erro:** `column "created_at" does not exist` → 500

**Correcção:** Usar `COALESCE(updated_at, created_at)` ou migration `ADD COLUMN created_at`.

---

### 2. WebSocket `/ws` sem autenticação

**Ficheiro:** `ws.js`

**Problema:** Qualquer cliente pode ligar e receber eventos `update`. Não valida JWT nem plugin key.

**Risco:** Information disclosure (saber quando há actividade no servidor).

---

### 3. Endpoints plugin públicos mesmo com `PLUGIN_API_KEY`

**Ficheiros:** `routes/players.js`

| Endpoint | Protegido? |
|----------|------------|
| `POST /playerJoin` | ❌ Público |
| `GET /player/:uuid` | ❌ Público |

**Problema:** Com key configurada, estes endpoints ignoram `PLUGIN_API_KEY`. Qualquer um pode consultar saldos ou registar players falsos.

---

## 🟠 Médios

### 4. `pluginAuth` exportado mas nunca usado

**Ficheiro:** `auth.js:36`

Middleware estrito (só plugin, sem fallback JWT) existe mas nenhuma route o importa. Código morto / confusão.

---

### 5. `POST /money/add` — 404 se player não existir

**Ficheiro:** `routes/economy.js`

Reference faz UPSERT (cria player). Backend actual só UPDATE → plugin recebe 404 em players novos.

---

### 6. Resposta vazia em `/money/add`

**Problema:** Plugin pode esperar `{ balance, coins }` (reference). Actual devolve `200` sem body.

**Impacto:** Compatibilidade parcial com plugins que leem resposta JSON.

---

### 7. `stats/sync` não sincroniza `rank` nem `world`

**Ficheiro:** `routes/stats.js:38-64`

Body aceita kills/playtime mas ignora rank — rank fica stale na DB.

---

### 8. Duplicação de definição `rank` no schema

**Ficheiro:** `database.js`

- CREATE TABLE: `rank TEXT NOT NULL DEFAULT 'bronze'`
- ALTER: `rank VARCHAR(50) DEFAULT 'bronze'`

Inofensivo se coluna já existe, mas inconsistente para novas instalações.

---

### 9. Pasta `reference/` no repositório

**Problema:** Código legado paralelo confunde deploy e documentação. Não é usado em runtime mas pode ser importado por engano.

---

### 10. Sem error handler global Express

**Problema:** Erros síncronos não capturados não passam por `internalError()`. Reference tinha `app.use((err, req, res, next) => ...)`.

---

## 🟡 Baixos

### 11. Sem rate limiting

Endpoints públicos (`/login`, `/playerJoin`, `/player/:uuid`) vulneráveis a brute-force e spam.

---

### 12. Sem testes automatizados

**Ficheiro:** `package.json` — só `"start": "node server.js"`. Zero scripts de teste.

---

### 13. `GET /logs` não existe

Logs são escritos mas dashboard não consegue listá-los via API.

---

### 14. Leaderboards incompletos

Colunas `kills` e `playtime` existem na DB mas faltam:

- `GET /top/kills`
- `GET /top/playtime`

---

### 15. `sqlPlayerSelect()` inclui `id` e `created_at`

Pode falhar em schemas legados sem essas colunas (mesmo problema #1).

---

### 16. JWT único admin

Um só par `ADMIN_USER`/`ADMIN_PASS` — sem multi-user, sem roles, sem refresh token.

---

### 17. Token em `localStorage`

Dashboard guarda JWT em `localStorage` — vulnerável a XSS (risco baixo se dashboard estática sem inputs externos).

---

## ✅ Resolvido (sessões anteriores)

| Problema | Estado |
|----------|--------|
| Rotas duplicadas em server.js | ✅ Removidas |
| `coins` hardcoded em server.js | ✅ Removido |
| Coluna `rank` inexistente | ✅ Migration ADD COLUMN |
| Coluna `coins` vs `balance` | ✅ resolvePlayerColumns() |
| Key inválida → 401 em vez de 403 | ✅ Corrigido |
| `/debug/columns` público | ✅ Protegido com JWT |

---

## Resumo por severidade

| Severidade | Count |
|------------|-------|
| 🔴 Crítico | 3 |
| 🟠 Médio | 7 |
| 🟡 Baixo | 7 |
| ✅ Resolvido | 6 |
