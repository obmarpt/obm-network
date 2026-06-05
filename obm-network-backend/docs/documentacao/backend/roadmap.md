# Roadmap técnico

Prioridades para completar e endurecer o backend OBM.

---

## 🔴 Prioridade ALTA

### R1 — Corrigir ORDER BY em GET /players

**Descrição:** Query falha se `created_at` não existir na DB de produção.

**Como implementar:**
1. Migration: `ALTER TABLE players ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT NOW()`
2. Alterar query: `ORDER BY COALESCE(updated_at, created_at) DESC NULLS LAST`
3. Testar em Render após deploy

**Ficheiros:** `database.js`, `routes/players.js`

---

### R2 — Definir `PLUGIN_API_KEY` em produção

**Descrição:** Sem a key, endpoints sensíveis ficam abertos (modo dev).

**Como implementar:**
1. Render → Environment → `PLUGIN_API_KEY=<string aleatória 32+ chars>`
2. Configurar mesmo valor no plugin Minecraft
3. Verificar logs `🔌 Plugin auth OK` no Render

**Ficheiros:** config plugin + Render dashboard

---

### R3 — UPSERT em POST /money/add

**Descrição:** Evitar 404 quando player ainda não existe na DB.

**Como implementar:**
```sql
INSERT INTO players (uuid, coins) VALUES ($1, $2)
ON CONFLICT (uuid) DO UPDATE SET coins = players.coins + EXCLUDED.coins
```
Manter resposta actual ou adicionar `{ balance, coins }` opcional.

**Ficheiros:** `routes/economy.js`

---

### R4 — Proteger ou limitar WebSocket

**Descrição:** `/ws` aberto a qualquer cliente.

**Como implementar (opção A — simples):**
- Validar token JWT via query `?token=` no handshake

**Opção B:**
- Aceitar só conexões com Origin do mesmo host

**Ficheiros:** `ws.js`, `public/dashboard.js`

---

## 🟠 Prioridade MÉDIA

### R5 — Endpoints `/top/kills` e `/top/playtime`

**Descrição:** Colunas existem, endpoints não.

**Como implementar:** Copiar padrão de `top/coins` em `routes/stats.js`. Auth JWT. LIMIT 10 + query param `?limit=`.

**Referência:** `reference/server/routes/top.js`

---

### R6 — GET /logs para dashboard

**Descrição:** Admin não vê histórico de acções in-game.

**Como implementar:**
```javascript
router.get('/logs', authMiddleware, async (req, res) => {
  const limit = Math.min(parseInt(req.query.limit) || 50, 100);
  // SELECT * FROM logs ORDER BY created_at DESC LIMIT $1
});
```

**Ficheiros:** `routes/logs.js`, `public/dashboard.js`

---

### R7 — Incluir `rank` em stats/sync

**Descrição:** Rank nunca actualizado via sync.

**Como implementar:** Adicionar `rank` ao body e ao UPSERT. Validar valores permitidos (bronze/silver/gold/platinum).

**Ficheiros:** `routes/stats.js`

---

### R8 — Resposta JSON em /money/add

**Descrição:** Compatibilidade plugin reference.

**Como implementar:**
```javascript
res.json({ ok: true, uuid, amount: parsedAmount, balance, coins: balance });
```
Manter `200` vazio como fallback se `Accept` não pedir JSON — ou sempre JSON.

**Ficheiros:** `routes/economy.js`

---

### R9 — Error handler global Express

**Como implementar:** Em `server.js` após routes:
```javascript
app.use((err, req, res, next) => {
  console.error('Erro:', err);
  res.status(500).json({ error: 'internal error' });
});
```

---

### R10 — Remover ou isolar pasta `reference/`

**Descrição:** Evitar confusão e imports acidentais.

**Como implementar:** Mover para branch arquivada ou renomear para `_reference/` com README "não usar".

---

### R11 — Índices em players para tops

**Como implementar:**
```sql
CREATE INDEX IF NOT EXISTS idx_players_coins ON players (coins DESC);
CREATE INDEX IF NOT EXISTS idx_players_emeralds ON players (emeralds DESC);
```
Usar colunas dinâmicas via `col('coins')` no CREATE INDEX após resolvePlayerColumns.

**Ficheiros:** `database.js`

---

## 🟡 Prioridade BAIXA

### R12 — Rate limiting

**Como implementar:** `express-rate-limit` em `/login` (5/min) e endpoints públicos.

---

### R13 — Testes automatizados

**Como implementar:** Jest ou supertest. Ver `testes.md`.

---

### R14 — Multi-admin / roles

**Como implementar:** Tabela `users` + bcrypt. Fora de scope actual (single admin env).

---

### R15 — Remover `pluginAuth` não usado ou aplicar

**Como implementar:** Usar em routes só-plugin OU remover export.

---

### R16 — Endpoint PATCH /player/:uuid/rank

**Descrição:** Dashboard poder alterar rank manualmente.

---

### R17 — Paginação em GET /players

**Descrição:** Com muitos players, resposta fica pesada.

**Como implementar:** `?page=1&limit=50`

---

## Timeline sugerida

| Semana | Items |
|--------|-------|
| 1 | R1, R2, R3 |
| 2 | R4, R5, R6 |
| 3 | R7, R8, R9, R11 |
| 4 | R12, R13, R10 |

---

## Definition of Done — backend "completo"

- [ ] Zero erros 500 em endpoints documentados
- [ ] `PLUGIN_API_KEY` activa em produção
- [ ] Plugin + dashboard testados end-to-end
- [ ] Leaderboards kills/playtime funcionais
- [ ] WebSocket autenticado
- [ ] Testes manuais de `testes.md` passam 100%
- [ ] Pasta reference isolada
- [ ] Logs consultáveis na dashboard
