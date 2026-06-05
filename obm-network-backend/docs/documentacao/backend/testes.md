# Testes — Plano completo

Suite de testes manuais para validar o backend OBM. Executar após cada deploy.

**Variáveis de teste:**

```
BASE=https://obm-network-backend.onrender.com
TOKEN=<JWT de POST /login>
PLUGIN_KEY=<PLUGIN_API_KEY>
UUID=550e8400-e29b-41d4-a716-446655440000
```

---

## 1. Testes de infraestrutura

### T1.1 — Health check

```bash
curl -s $BASE/health | jq .
```

| Esperado | |
|----------|---|
| Status | 200 |
| Body | `{ "ok": true, "service": "obm-backend" }` |

### T1.2 — Dashboard estática

```bash
curl -s -o /dev/null -w "%{http_code}" $BASE/
```

| Esperado | 200 (HTML) |

### T1.3 — WebSocket connect

```javascript
// Browser console em $BASE
const ws = new WebSocket('wss://obm-network-backend.onrender.com/ws');
ws.onopen = () => console.log('OK');
ws.onerror = (e) => console.error('FAIL', e);
```

| Esperado | Conexão aberta |

---

## 2. Testes de autenticação

### T2.1 — Login válido

```bash
curl -s -X POST $BASE/login \
  -H "Content-Type: application/json" \
  -d '{"username":"ADMIN_USER","password":"ADMIN_PASS"}'
```

| Esperado | 200 + `token` + `expiresIn: "2h"` |

### T2.2 — Login inválido

```bash
curl -s -X POST $BASE/login \
  -d '{"username":"admin","password":"wrong"}'
```

| Esperado | 401 `{ "error": "Invalid credentials" }` |

### T2.3 — JWT em falta

```bash
curl -s -o /dev/null -w "%{http_code}" $BASE/players
```

| Esperado | 401 |

### T2.4 — JWT inválido

```bash
curl -s -o /dev/null -w "%{http_code}" $BASE/players \
  -H "Authorization: Bearer invalid.token.here"
```

| Esperado | 403 |

### T2.5 — Plugin key inválida

```bash
curl -s -o /dev/null -w "%{http_code}" -X POST $BASE/money/add \
  -H "Content-Type: application/json" \
  -H "X-Plugin-Key: wrong-key" \
  -d "{\"uuid\":\"$UUID\",\"amount\":1}"
```

| Esperado | 403 (com PLUGIN_API_KEY definida) |

### T2.6 — Plugin key válida

```bash
curl -s -o /dev/null -w "%{http_code}" -X POST $BASE/money/add \
  -H "Content-Type: application/json" \
  -H "X-Plugin-Key: $PLUGIN_KEY" \
  -d "{\"uuid\":\"$UUID\",\"amount\":1}"
```

| Esperado | 200 ou 404 (se player não existe) |

---

## 3. Testes endpoints — Players

### T3.1 — Player join

```bash
curl -s -o /dev/null -w "%{http_code}" -X POST $BASE/playerJoin \
  -H "Content-Type: application/json" \
  -d "{\"uuid\":\"$UUID\",\"name\":\"TestPlayer\",\"world\":\"world_nether\"}"
```

| Esperado | 200 |

### T3.2 — Player join UUID inválido

```bash
curl -s -X POST $BASE/playerJoin \
  -d '{"uuid":"short","name":"X","world":"w"}'
```

| Esperado | 400 `{ "error": "uuid inválido" }` |

### T3.3 — GET player existente

```bash
curl -s $BASE/player/$UUID | jq .
```

| Esperado | 200 com `coins`, `balance`, `money`, `emeralds`, `rank` |

### T3.4 — GET player inexistente

```bash
curl -s -o /dev/null -w "%{http_code}" \
  $BASE/player/00000000-0000-0000-0000-000000000000
```

| Esperado | 404 |

### T3.5 — GET players (JWT)

```bash
curl -s $BASE/players -H "Authorization: Bearer $TOKEN" | jq 'length'
```

| Esperado | 200 + array |

---

## 4. Testes endpoints — Economia

### T4.1 — Money add positivo

```bash
# Pré: T3.1 player join
curl -s -o /dev/null -w "%{http_code}" -X POST $BASE/money/add \
  -H "Content-Type: application/json" \
  -H "X-Plugin-Key: $PLUGIN_KEY" \
  -d "{\"uuid\":\"$UUID\",\"amount\":500}"
```

| Esperado | 200 |

Verificar saldo:
```bash
curl -s $BASE/player/$UUID | jq .coins
# → 500 (ou anterior + 500)
```

### T4.2 — Money add negativo (remover)

```bash
curl -s -o /dev/null -w "%{http_code}" -X POST $BASE/money/add \
  -H "Authorization: Bearer $TOKEN" \
  -d "{\"uuid\":\"$UUID\",\"amount\":-100}"
```

| Esperado | 200 |

### T4.3 — Money add sem amount

```bash
curl -s -X POST $BASE/money/add \
  -H "Authorization: Bearer $TOKEN" \
  -d "{\"uuid\":\"$UUID\"}"
```

| Esperado | 400 `amount obrigatório` |

### T4.4 — Money add player inexistente

```bash
curl -s -o /dev/null -w "%{http_code}" -X POST $BASE/money/add \
  -H "X-Plugin-Key: $PLUGIN_KEY" \
  -d '{"uuid":"00000000-0000-0000-0000-000000000001","amount":10}'
```

| Esperado | 404 |

---

## 5. Testes endpoints — Comandos

### T5.1 — Enfileirar comando

```bash
curl -s -o /dev/null -w "%{http_code}" -X POST $BASE/command \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"command":"say Teste API"}'
```

| Esperado | 200 |

### T5.2 — Consumir comandos (plugin poll)

```bash
curl -s $BASE/commands -H "X-Plugin-Key: $PLUGIN_KEY" | jq .
```

| Esperado | 200 + array com comando enfileirado |

### T5.3 — Segunda poll vazia

```bash
curl -s $BASE/commands -H "X-Plugin-Key: $PLUGIN_KEY" | jq 'length'
```

| Esperado | 0 (já executados) |

### T5.4 — Comando vazio

```bash
curl -s -X POST $BASE/command \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"command":""}'
```

| Esperado | 400 |

---

## 6. Testes endpoints — Logs

### T6.1 — POST log

```bash
curl -s -o /dev/null -w "%{http_code}" -X POST $BASE/log \
  -H "X-Plugin-Key: $PLUGIN_KEY" \
  -d '{"staff":"Admin","action":"TEST","target":"Steve","value":"unit-test"}'
```

| Esperado | 200 |

### T6.2 — Log sem staff

```bash
curl -s -X POST $BASE/log \
  -H "X-Plugin-Key: $PLUGIN_KEY" \
  -d '{"action":"TEST"}'
```

| Esperado | 400 |

---

## 7. Testes endpoints — Stats

### T7.1 — Stats global

```bash
curl -s $BASE/stats/global -H "Authorization: Bearer $TOKEN" | jq .
```

| Esperado | `{ totalPlayers, totalCoins, avgCoins }` |

### T7.2 — Top coins

```bash
curl -s $BASE/top/coins -H "Authorization: Bearer $TOKEN" | jq 'length'
```

| Esperado | ≤ 10 |

### T7.3 — Stats sync

```bash
curl -s -X POST $BASE/stats/sync \
  -H "X-Plugin-Key: $PLUGIN_KEY" \
  -d "{\"uuid\":\"$UUID\",\"name\":\"TestPlayer\",\"coins\":1000,\"emeralds\":50,\"kills\":10,\"playtime\":3600}"
```

| Esperado | 200 `{ "ok": true, "uuid": "..." }` |

---

## 8. Testes de erro (403, 500)

### T8.1 — 403 plugin key

Ver T2.5

### T8.2 — 403 JWT expirado

Usar token com mais de 2h → GET /players → 403

### T8.3 — 500 simulado (debug columns sem auth após fix)

```bash
curl -s -o /dev/null -w "%{http_code}" $BASE/debug/columns
```

| Esperado | 401 (protegido) |

### T8.4 — DB offline

Parar PostgreSQL → GET /health → 503

---

## 9. Testes WebSocket

### T9.1 — Broadcast on money add

1. Abrir dashboard (login)
2. Abrir DevTools → Network → WS
3. Executar T4.1 (money add)
4. Verificar mensagem WS: `{ "type": "update" }`
5. Tabela actualiza automaticamente

### T9.2 — Broadcast on player join

1. Dashboard aberta
2. Executar T3.1
3. Verificar WS message + reload

### T9.3 — Reconnect

1. Desligar WiFi 5s
2. Religar
3. Dashboard reconecta WS em ~3s (dashboard.js)

---

## 10. Testes integração plugin (E2E)

| # | Cenário | Passos | Esperado |
|---|---------|--------|----------|
| E1 | Join in-game | Player entra servidor | Row em DB, name/world actualizados |
| E2 | Kill reward | Evento coins in-game | POST /money/add, saldo sobe |
| E3 | Admin ban | Dashboard ban | Comando na fila, plugin executa |
| E4 | Command poll | Plugin tick | GET /commands, comando desaparece da fila |
| E5 | Staff log | Acção admin in-game | POST /log, Discord recebe embed |
| E6 | Rank display | GET /player/:uuid | rank presente na resposta |

---

## 11. Checklist regressão pós-deploy

```
[ ] T1.1 Health OK
[ ] T2.1 Login OK
[ ] T2.5 Plugin key 403 (key errada)
[ ] T3.1 Player join OK
[ ] T3.3 GET player aliases OK
[ ] T4.1 Money add OK
[ ] T5.1-T5.3 Command queue OK
[ ] T6.1 Log OK
[ ] T7.1 Stats OK
[ ] T9.1 WebSocket update OK
[ ] Dashboard login + acções OK
[ ] Zero erros 500 nos logs Render (15 min)
```

---

## 12. Testes automatizados (futuro)

Template supertest para CI:

```javascript
// tests/health.test.js (NÃO IMPLEMENTADO)
const request = require('supertest');
const app = require('../server');

test('GET /health returns ok', async () => {
  const res = await request(app).get('/health');
  expect(res.status).toBe(200);
  expect(res.body.ok).toBe(true);
});
```

Adicionar quando R13 do roadmap for implementado.

---

## Matriz resumo

| Área | Testes | Críticos |
|------|--------|----------|
| Infra | T1.x | T1.1 |
| Auth | T2.x | T2.1, T2.5 |
| Players | T3.x | T3.1, T3.3 |
| Economia | T4.x | T4.1, T4.4 |
| Comandos | T5.x | T5.1, T5.2 |
| Stats | T7.x | T7.1 |
| WS | T9.x | T9.1 |
| E2E | E1-E6 | E1, E4 |
