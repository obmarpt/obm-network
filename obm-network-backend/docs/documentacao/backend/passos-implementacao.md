# Passos de implementação — Guia completo

Ordem recomendada para terminar e validar o backend.

---

## Fase 1 — Configuração ambiente (Render)

### Passo 1.1 — Variáveis obrigatórias

No Render → Service → Environment:

```
DATABASE_URL=postgresql://...
JWT_SECRET=<string aleatória 64 chars>
ADMIN_USER=admin
ADMIN_PASS=<password forte>
PORT=10000
```

### Passo 1.2 — Variáveis recomendadas

```
PLUGIN_API_KEY=<string aleatória 32+ chars>
DISCORD_WEBHOOK_URL=https://discord.com/api/webhooks/...
COINS_COLUMN=balance     # só se DB usar nome diferente
```

### Passo 1.3 — Deploy

- Build: `npm install`
- Start: `npm start`
- Verificar logs: `✅ DB conectada`, `✅ Auth JWT configurado`, `✅ Plugin API key configurada`

---

## Fase 2 — Configurar auth

### Passo 2.1 — Testar login dashboard

1. Abrir `https://obm-network-backend.onrender.com/`
2. Login com `ADMIN_USER` / `ADMIN_PASS`
3. Confirmar tabela de players carrega

### Passo 2.2 — Configurar plugin key

1. Copiar `PLUGIN_API_KEY` do Render
2. Colocar no config do plugin Minecraft (header `X-Plugin-Key`)
3. Reiniciar plugin

### Passo 2.3 — Verificar logs auth

No Render logs, após request plugin:
```
🔌 Plugin auth OK: POST /money/add
```

Se key inválida:
```
⚠️ Plugin key inválida: POST /money/add
```

---

## Fase 3 — Proteger endpoints

### Passo 3.1 — Confirmar matriz de auth

| Endpoint | Plugin key | JWT | Público |
|----------|:----------:|:---:|:-------:|
| POST /money/add | ✅ | ✅ | dev only |
| POST /log | ✅ | ✅ | dev only |
| POST /command | ✅ | ✅ | dev only |
| GET /commands | ✅ | ✅ | dev only |
| GET /players | ❌ | ✅ | ❌ |
| POST /playerJoin | ❌ | ❌ | ✅ |

### Passo 3.2 — Remover modo dev em produção

Garantir `PLUGIN_API_KEY` **sempre** definida em Render.

---

## Fase 4 — Corrigir queries DB

### Passo 4.1 — Verificar colunas

```bash
curl -H "Authorization: Bearer <token>" \
  https://obm-network-backend.onrender.com/debug/columns
```

Confirmar `mapped.coins` aponta para coluna correcta.

### Passo 4.2 — Corrigir created_at (se 500 em /players)

Executar manualmente ou via migration:
```sql
ALTER TABLE players ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT NOW();
```

### Passo 4.3 — Confirmar rank

```sql
ALTER TABLE players ADD COLUMN IF NOT EXISTS rank VARCHAR(50) DEFAULT 'bronze';
```
(Já feito automaticamente em `initDb()` — reiniciar serviço)

---

## Fase 5 — Testar DB

### Passo 5.1 — Health

```bash
curl https://obm-network-backend.onrender.com/health
# → {"ok":true,"service":"obm-backend"}
```

### Passo 5.2 — Player join (plugin)

```bash
curl -X POST .../playerJoin \
  -H "Content-Type: application/json" \
  -d '{"uuid":"550e8400-e29b-41d4-a716-446655440000","name":"Test","world":"world"}'
# → 200
```

### Passo 5.3 — Money add

```bash
curl -X POST .../money/add \
  -H "Content-Type: application/json" \
  -H "X-Plugin-Key: YOUR_KEY" \
  -d '{"uuid":"550e8400-e29b-41d4-a716-446655440000","amount":100}'
# → 200
```

### Passo 5.4 — Verificar player

```bash
curl .../player/550e8400-e29b-41d4-a716-446655440000
# → coins, balance, money, rank
```

---

## Fase 6 — Validar dashboard

### Passo 6.1 — Login e stats

- [ ] Stats globais mostram números
- [ ] Top coins/emeralds populados
- [ ] Tabela players com rank badges

### Passo 6.2 — Acções admin

- [ ] Dar coins a player
- [ ] Remover coins
- [ ] Kick / Ban / Freeze (comando na fila)
- [ ] Broadcast manual

### Passo 6.3 — WebSocket live

1. Abrir dashboard
2. Trigger `POST /money/add` via plugin ou curl
3. Tabela actualiza sem esperar 10s

### Passo 6.4 — Logout / sessão expirada

- [ ] Token inválido → redirect login
- [ ] Logout limpa localStorage

---

## Fase 7 — Validar plugin Minecraft

### Passo 7.1 — Checklist integração

- [ ] Player join envia name/world/uuid
- [ ] GET /player/:uuid lê saldo
- [ ] Money events com plugin key
- [ ] GET /commands polling funciona
- [ ] Comandos executados no servidor MC

### Passo 7.2 — Monitorizar Render logs

Procurar erros `Erro (money/add):`, `column does not exist`.

---

## Fase 8 — Limpeza final

- [ ] Confirmar `server.js` só tem `/health` + `app.use(routes)`
- [ ] Remover/isolar pasta `reference/`
- [ ] Executar todos os testes de `testes.md`
- [ ] Documentar `PLUGIN_API_KEY` no README do plugin

---

## Rollback

Se deploy falhar:

1. Render → Manual Deploy → deploy anterior
2. Verificar `DATABASE_URL` intacta
3. Testar `/health` antes de re-testar endpoints
