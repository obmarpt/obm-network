# Fluxos de operação

## 1. Player Join

```mermaid
sequenceDiagram
    participant MC as Plugin Minecraft
    participant API as Express /playerJoin
    participant DB as PostgreSQL
    participant WS as WebSocket

    MC->>API: POST { uuid, name, world }
    API->>API: validateUuid (lowercase, len≥32)
    API->>DB: INSERT ... ON CONFLICT UPDATE name, world
    API->>WS: broadcast { type: update }
    API->>MC: 200 OK
    WS->>Dashboard: update event
    Dashboard->>API: GET /players (JWT)
```

**Auth:** público — plugin não precisa de key para join.

---

## 2. Ganho de coins

```mermaid
sequenceDiagram
    participant MC as Plugin / Dashboard
    participant Auth as pluginOrAuthMiddleware
    participant API as POST /money/add
    participant DB as PostgreSQL
    participant WS as WebSocket

    MC->>Auth: X-Plugin-Key ou Bearer JWT
    alt key inválida
        Auth->>MC: 403 Forbidden
    end
    Auth->>API: next()
    API->>DB: UPDATE coins = coins + amount
    alt player não existe
        API->>MC: 404
    end
    API->>WS: broadcast update
    API->>MC: 200
```

**Nota:** Dashboard usa amount negativo para remover coins.

---

## 3. Execução de comandos remotos

```mermaid
sequenceDiagram
    participant Dash as Dashboard
    participant API as POST /command
    participant DB as commands
    participant MC as Plugin (poll)
    participant Srv as Servidor MC

    Dash->>API: POST { command: "kick Steve" } + JWT
    API->>DB: INSERT executed=false
    API->>Dash: 200

    loop polling
        MC->>API: GET /commands + Plugin-Key
        API->>DB: UPDATE executed=true RETURNING rows
        API->>MC: [{ id, command, created_at }]
        MC->>Srv: executa comando
    end
```

**Atómico:** GET marca todos pendentes como executados numa única query.

---

## 4. Dashboard load

```mermaid
sequenceDiagram
    participant Browser
    participant API
    participant WS

    Browser->>API: POST /login
    API->>Browser: { token }
    Browser->>Browser: localStorage obm_token
    Browser->>WS: connect wss://host/ws
    Browser->>API: GET /stats/global + JWT
    Browser->>API: GET /top/coins + JWT
    Browser->>API: GET /top/emeralds + JWT
    Browser->>API: GET /players + JWT
    Browser->>Browser: render tabela + stats

    loop cada 10s
        Browser->>API: refreshAll()
    end

    Note over WS,Browser: onmessage update → reload imediato
```

---

## 5. Admin log + Discord

```
POST /log { staff, action, target, value }
  → INSERT logs
  → notifyAdminLog() → Discord webhook (async, não bloqueia)
  → 200
```

---

## 6. Stats sync (plugin → leaderboard)

```
POST /stats/sync { uuid, name, coins, emeralds, kills, playtime }
  → UPSERT players (valores absolutos)
  → broadcast update
  → { ok: true, uuid }
```

Usado quando o plugin envia snapshot completo do player (não delta).

---

## 7. Autenticação combinada

```
Request com PLUGIN_API_KEY definida:

  Header X-Plugin-Key presente?
    ├─ SIM + válida → ✅ plugin
    ├─ SIM + inválida → ❌ 403
    └─ NÃO → tenta JWT (dashboard)

  Sem PLUGIN_API_KEY (dev):
    → ✅ acesso aberto
```
