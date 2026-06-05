# Leaderboards globais

## Fluxo

```
PostgreSQL → GET /top/* → LeaderboardService (cache 60s) → HologramManager → DecentHolograms
```

## Backend (Render)

Deploy `obm-network-backend/` e executar `schema.sql`.

Colunas usadas nos tops: `coins`, `emeralds`, `kills`, `playtime`.

## Plugin

| Classe | Função |
|--------|--------|
| `LeaderboardService` | `fetchTopCoins()`, cache, fallback `data.yml` |
| `BackendTopClient` | HTTP GET async/sync |
| `HologramManager` | Hologramas no lobby |

## Config (`OBM-Core/config.yml`)

```yaml
remote-leaderboard:
  enabled: true
  refresh-seconds: 60
  holograms:
    list:
      kills:
        x: 20
        y: 166
        z: 0
```

## Exemplo código

```java
LeaderboardService lb = OBMCorePlugin.get().getLeaderboardService();
lb.fetchTopCoins(list -> {
    for (TopPlayerEntry e : list) {
        // rank, name, value
    }
});
List<TopPlayerEntry> cached = lb.getCachedTop(GlobalTopType.COINS, 10);
```

## Requisitos

- **DecentHolograms** instalado
- Mundo `Lobby` carregado
- `backend.enabled: true` para dados da API (senão fallback local)
