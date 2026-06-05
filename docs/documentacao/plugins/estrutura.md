# Estrutura dos plugins

## Módulos Maven (`omd-network-parent`)

| Plugin | JAR | Dependências hard | Função principal |
|--------|-----|-------------------|----------------|
| **OBM-Core** | OBM-Core | — | Núcleo: dados, economia global (Emeralds), backend, combat, seasons, admin, hologramas, PAPI |
| **OBM-SMP** | OBM-SMP | OBM-Core | Rush SMP: coins, shop, sell, auction, market, ranks/level SMP |
| **OBM-TierSpace** | OBM-TierSpace | OBM-Core | PvP ranked: filas, ELO, kits, seasons ranked |
| **OBM-Lobby** | OBM-Lobby | OBM-Core | Hub: `/lobby`, menu GUI, retenção UX |
| **OBM-UHC** | OBM-UHC | OBM-Core | Hardcore: vidas, timer, revive staff, stats UHC |
| **OBM-Inventory** | OBM-Inventory | OBM-Core | Inventário por mundo (cache complementar) |

**Ordem de arranque recomendada:** Core → SMP, TierSpace, Inventory, UHC → **Lobby por último**.

## Diagrama de dependências

```
                    ┌─────────────┐
                    │  OBM-Core   │
                    └──────┬──────┘
           ┌───────────────┼───────────────┬──────────────┐
           ▼               ▼               ▼              ▼
     ┌──────────┐   ┌────────────┐   ┌──────────┐  ┌────────────┐
     │ OBM-SMP  │   │ OBM-TierSpace│  │ OBM-UHC  │  │ OBM-Inventory│
     └────┬─────┘   └────────────┘   └────┬─────┘  └────────────┘
          │                                │
          └────────────┬───────────────────┘
                       ▼
                ┌─────────────┐
                │  OBM-Lobby  │
                └─────────────┘
```

## Plugins externos (runtime, pasta `plugins/`)

| Plugin | Usado por |
|--------|-----------|
| **Citizens** | NPCs lobby → `NpcModeRouter` (Core) |
| **DecentHolograms** | `HologramService`, tops globais |
| **PlaceholderAPI** | `%obm_*%`, TAB, hologramas estáticos |
| **LuckPerms** | Permissões (`obm.*`, `obm.smp.*`, staff) |
| **Vault** | Ponte economia SMP (opcional) |
| **Multiverse-Core** | Múltiplos mundos |
| **Spartan / GrimAC** | Anti-cheat (TierSpace) |
| **Essentials** | Homes/TPA — economia desactivada |

## Como interagem

### Dados persistentes

- **Ficheiro principal:** `plugins/OBM-Core/data.yml` (`DataStore`) — stats, inventários, posições, chaves legado.
- **Economia Rush (coins):** com `backend.player-storage.enabled`, fonte de verdade em **PostgreSQL** + **cache RAM** (`PlayerCache` / `PlayerData`).
- **Emeralds (network):** `GlobalEconomyService` + `data.yml` (não migrado para API coins).

### Bridges no Core

| Bridge | Liga |
|--------|------|
| `EconomyBridge` | SMP ↔ cache/backend ou DataStore |
| `SMPBridge` | Level/rank SMP para Hardcore unlock |
| `TierSpaceBridge` | Rating/stats TierSpace |
| `BackendStorageHook` | SMP notifica alterações de saldo ao cache |

### Dois sistemas HTTP distintos

1. **Render** (`BackendService`, `HttpUtils`) — jogadores, coins, logs, tops, commands remotos.
2. **minespace-admin** (`AdminBridgeService`) — audit staff, heartbeat, painel web (opcional).

## Mundos (`WorldModeService`)

Configurados em `OBM-Core/config.yml`:

| Tipo | Config | Default |
|------|--------|---------|
| Rush SMP | `smp-worlds` | world, world_nether, world_the_end |
| Hardcore | `uhc-worlds` | UHC, UHC_nether, UHC_the_end |
| TierSpace hub | `ranked-worlds` | rankedSpawn |
| Lobby | `lobby.world` | Lobby |

## Pastas úteis no repo

| Pasta | Conteúdo |
|-------|----------|
| `OBM-*/src/main/java` | Código-fonte |
| `PluginsCompilados/` | JARs após `mvn install` |
| `obm-network-backend/` | API Node + PostgreSQL |
| `minespace-admin/` | Painel admin Node |
| `server-config/` | Templates anticheat, mythicmobs |
