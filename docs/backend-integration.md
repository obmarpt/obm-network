# OBM-Core ↔ Backend (Render)

## Classes

| Classe | Função |
|--------|--------|
| `HttpUtils` | GET/POST JSON, erros, logs |
| `BackendConfig` | URL, timeouts, poll |
| `BackendService` | Orquestração (join, log, commands, stats sync) |
| `PlayerStatsTracker` | Eventos join/quit, kills, coin_gain → HTTP async |
| `PlayerStats` | Snapshot coins / playtime / kills |
| `EconomyManager` | `/money/add`, `GET /player/:uuid` |
| `PlayerJoinApiListener` | POST `/playerJoin` no join |
| `BackendKillListener` | POST `/log` em kills (exemplo) |

## Config

```yaml
backend:
  enabled: true
  base-url: https://obm-network-backend.onrender.com
  plugin-key: your-secret-key-here
  debug: false
  command-poll-seconds: 5
```

Todas as requests passam por `HttpUtils` e enviam o header `X-Plugin-Key` (valor de `backend.plugin-key`).

## Exemplos no código

```java
BackendService api = OBMCorePlugin.get().getBackendService();

// Join — automático via PlayerJoinApiListener

// Coins
api.addCoins(player, 500);

// Dados do jogador
api.getPlayerData(player.getUniqueId(), data -> {
    if (data != null) {
        player.sendMessage("§eCoins DB: " + data.coins() + " | Rank: " + data.rank());
    }
});

// Log staff
api.sendLog(staffPlayer, "BAN", targetName, "cheating");

// Comandos remotos — polling automático a cada 5s (GET /commands)
```

## Endpoints usados

- `POST /playerJoin` — uuid, name (trim), world
- `GET /player/:uuid` — coins, emeralds, rank
- `POST /money/add` — body **apenas** `{ "uuid", "amount" }` (nunca `coins`)
- `GET /player/:uuid` — plugin lê saldo de `balance`, `money` ou `coins` (primeiro presente) — deltas de coins (cache sync)
- `POST /stats/sync` — uuid, name, coins, emeralds, kills, playtime — leaderboards
- `POST /log` — staff, action, target, value (join, quit, coin_gain, admin)
- `GET /commands` — executados na main thread via console
- `GET /top/coins` — top 10 global (JSON `entries[]`)
- `GET /top/emeralds`
- `GET /top/kills`
- `GET /top/playtime`

## Leaderboards no lobby

- `LeaderboardService` — cache RAM, refresh 60s (async HTTP)
- `HologramManager` — DecentHolograms em `remote-leaderboard.holograms` (config.yml)
- Backend Node: pasta `obm-network-backend/` + `schema.sql`

## Segurança plugin

- UUID validado antes de enviar
- Strings trim; campos obrigatórios não vazios
- Tudo async exceto `dispatchCommand` (main thread)
- Falhas HTTP nunca propagam excepção ao servidor
