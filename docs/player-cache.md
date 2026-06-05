# Player Cache (zero lag)

## Princípio

| Momento | HTTP | Thread |
|---------|------|--------|
| Gameplay (coins, shop, kill reward) | **Nunca** | Main — só `PlayerCache` |
| Join | GET `/player/:uuid` + POST `/playerJoin` | Async → Main |
| Sync 30s | POST `/money/add` (batch deltas) | Async |
| Quit | Flush final + remove cache | Async → Main |
| Admin log | POST `/log` | Async |

## Classes

- `PlayerData` — coins, emeralds, rank, `pendingCoinDelta`
- `PlayerCache` — `Map<UUID, PlayerData>`
- `PlayerCacheService` — gameplay + load/unload
- `PlayerCacheSyncTask` — sync periódico
- `BackendService` — `fetchPlayer`, `sendCoinsSync`, `sendLog`
- `EconomyManager` — HTTP de baixo nível

## Exemplo gameplay

```java
PlayerCacheService cache = OBMCorePlugin.get().getPlayerCacheService();
PlayerCache map = cache.getCache();

PlayerData data = map.get(player.getUniqueId()).orElse(null);
if (data != null) {
    data.addCoins(100);  // só RAM — sync em 30s
    player.sendMessage("Saldo: " + data.getCoins());
}
```

## Config

```yaml
backend:
  player-storage:
    enabled: true
    sync-interval-seconds: 30
    fallback-datastore: true
```
