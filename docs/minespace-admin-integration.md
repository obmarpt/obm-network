# Integração MineSpace Admin (Web + Discord)

## Componentes

| Pasta | Função |
|-------|--------|
| `minespace-admin/` | API Node.js + dashboard + Discord webhooks |
| `OBM-Core/.../bridge/` | Cliente bridge (heartbeat, eventos, comandos) |

## Quick start

1. **API** (VPS ou PC local):
   ```bash
   cd minespace-admin
   cp .env.example .env
   npm install && npm start
   ```

2. **Servidor Minecraft** — `plugins/OBM-Core/config.yml`:
   ```yaml
   admin-bridge:
     enabled: true
     url: http://IP_DA_API:3040
     api-key: <igual a BRIDGE_API_KEY no .env>
   ```

3. **Dashboard**: `http://IP:3040` — login `admin` / password do `.env`

## Fluxo de comandos (ban/kick/give)

1. Staff clica no dashboard → `POST /api/commands`
2. Comando fica em `pending_commands` (SQLite)
3. OBM-Core faz poll `GET /api/bridge/commands` a cada 2s
4. Executa no thread principal (Bukkit)
5. Confirma `POST /api/bridge/commands/:id/complete`

## Discord

Configurar no `.env`:

- `DISCORD_WEBHOOK_URL` — fallback global
- `DISCORD_WEBHOOK_ADMIN` — acções staff
- `DISCORD_WEBHOOK_ECONOMY` — coins/emeralds
- `DISCORD_WEBHOOK_ANTICHEAT` — flags AC
- `DISCORD_WEBHOOK_PUNISH` — ban/kick

Todos os `AdminAuditLog` do OBM-Core são reencaminhados automaticamente quando o bridge está activo.

Para anticheat TierSpace, chamar manualmente:

```java
OBMCorePlugin.get().getAdminBridgeService().publishAnticheat(player, check, detail, vl);
```

## Produção

- HTTPS reverse proxy (nginx) na API
- Não expor `:3040` publicamente sem firewall — usar VPN ou IP allowlist
- Rotacionar `JWT_SECRET` e `BRIDGE_API_KEY`
- Alterar `ADMIN_PASS` antes do primeiro deploy
