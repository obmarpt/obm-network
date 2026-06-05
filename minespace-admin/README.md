# MineSpace Admin (Web + Discord + API)

Painel externo para administração do servidor Minecraft, integrado com **OBM-Core** via bridge HTTP.

## Arquitectura

```
┌─────────────┐     REST/WS      ┌──────────────────┐     X-Bridge-Key    ┌─────────────┐
│  Dashboard  │ ◄──────────────► │  minespace-admin │ ◄──────────────────► │  OBM-Core   │
│  (browser)  │                  │  (Node.js :3040) │                      │  (Spigot)   │
└─────────────┘                  └────────┬─────────┘                      └─────────────┘
                                          │
                                          ▼ Discord webhooks
                                   ┌──────────────┐
                                   │   Discord    │
                                   └──────────────┘
```

## Instalação

```bash
cd minespace-admin
cp .env.example .env
# Editar JWT_SECRET, BRIDGE_API_KEY, DISCORD_WEBHOOK_*
npm install
npm start
```

Dashboard: `http://localhost:3040`  
Login default: `admin` / `changeme` (alterar no `.env`)

## API (JWT — header `Authorization: Bearer <token>`)

| Método | Path | Descrição |
|--------|------|-----------|
| POST | `/api/auth/login` | Login |
| GET | `/api/stats/live` | TPS, online, economia |
| GET | `/api/players` | Lista online |
| GET | `/api/players/:uuid` | Perfil (snapshot) |
| GET | `/api/economy` | Resumo + tops |
| GET | `/api/logs` | Staff audit logs |
| POST | `/api/commands` | Fila comando (kick, ban, give_*) |
| GET | `/api/access-logs` | Logs de acesso web (admin) |

## Bridge (servidor Minecraft — header `X-Bridge-Key`)

| Método | Path | Descrição |
|--------|------|-----------|
| POST | `/api/bridge/heartbeat` | Stats live |
| POST | `/api/bridge/event` | Evento + Discord |
| GET | `/api/bridge/commands` | Poll comandos pendentes |
| POST | `/api/bridge/commands/:id/complete` | Ack comando |

## WebSocket

`ws://host:3040/ws` — push de `stats` e `event`.

## OBM-Core

Em `plugins/OBM-Core/config.yml`:

```yaml
admin-bridge:
  enabled: true
  url: http://127.0.0.1:3040
  api-key: <mesmo BRIDGE_API_KEY do .env>
  heartbeat-seconds: 5
  command-poll-seconds: 2
```

## Comandos remotos

| type | payload |
|------|---------|
| `kick` | `{ "reason": "..." }` |
| `ban` | `{ "reason": "..." }` |
| `give_coins` | `{ "amount": 1000 }` |
| `give_emeralds` | `{ "amount": 50 }` |

## Segurança

- JWT 12h, roles `admin` / `mod`
- Bridge isolado por API key (não expor à internet sem firewall/VPN)
- Logs de login e acções web em SQLite (`data/admin.db`)
- Alterar passwords default antes de produção

## Discord

Variáveis opcionais por canal: `DISCORD_WEBHOOK_ADMIN`, `DISCORD_WEBHOOK_ECONOMY`, `DISCORD_WEBHOOK_ANTICHEAT`, `DISCORD_WEBHOOK_PUNISH`.

Eventos OBM-Core (`ADMIN_*`, `ANTICHEAT`, punições) são reencaminhados automaticamente.
