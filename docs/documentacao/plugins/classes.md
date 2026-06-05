# Classes — índice por módulo

Documentação por **pacote e responsabilidade**, não lista exaustiva de ~350 ficheiros `.java`. Para detalhe, abrir o pacote no IDE.

---

## OBM-Core (`com.obm.network.core`) — ~182 classes

### Bootstrap

| Classe | Função | Métodos / notas |
|--------|--------|-----------------|
| `OBMCorePlugin` | Arranque, wiring de serviços | `onEnable`, getters: `getDataStore`, `getBackendService`, `getPlayerCacheService`, `getWorldModeService` |

### `backend` — API Render

| Classe | Função | Métodos principais |
|--------|--------|-------------------|
| `BackendService` | Orquestra HTTP: join, poll commands, logs | `pollRemoteCommandsAsync`, `postLogAsync`, `syncMoneyBatch` |
| `HttpUtils` | GET/POST com `X-Plugin-Key` | `get`, `post` |
| `BackendConfig` | URLs, timeouts, plugin-key | `url(path)`, `getPluginKey` |
| `EconomyManager` | `POST /money/add`, `GET /player/:uuid` | `addCoinsAsync`, `fetchPlayerAsync` |
| `BackendJsonParser` | Parse saldo (`balance`/`money`/`coins`) | `parseBalance` |
| `BackendStatsClient` | `POST /stats/sync` | `syncStats` |
| `BackendTopClient` | `GET /top/*` | `fetchTop` |
| `BackendPlayerStorage` | Implementação `PlayerStorage` remota | `load`, `withdrawCoins`, `depositCoins` |

### `cache` — economia Rush em RAM

| Classe | Função | Métodos principais |
|--------|--------|-------------------|
| `PlayerData` | Estado sessão | `addCoins`, `removeCoins`, `getPendingCoinDelta` |
| `PlayerCache` | Map UUID → dados | `getOrNull`, `put` |
| `PlayerCacheService` | API pública cache | `depositCoins`, `withdrawCoins`, `loadPlayerAsync`, `flushOnQuit` |
| `PlayerCacheSyncTask` | Runnable 30s | sync deltas positivos |

### `npc` + listeners

| Classe | Função |
|--------|--------|
| `NpcModeRouter` | `handleClick`, `joinRush`, `joinTierSpace`, `handleHardcore` |
| `NPCClickListener` | Citizens → router + debounce |

### `integration` — bridges para outros plugins

| Classe | Função |
|--------|--------|
| `EconomyBridge` | Saldo coins (cache ou DataStore) |
| `RemoteEconomyBridge` | Delega a `BackendPlayerStorage` |
| `SMPBridge` | Level/rank SMP para Hardcore unlock |
| `TierSpaceBridge` | Rating TierSpace |
| `BackendStorageHook` | SMP notifica alterações saldo |

### `storage`

| Classe | Função |
|--------|--------|
| `DataStore` | YAML `data.yml` — stats, inventários, vidas UHC |
| `GlobalStorageListener` | Persistir inventário/posição por mundo |

### `hardcore` / `location` / `world`

| Classe | Função |
|--------|--------|
| `HardcoreUnlockService` | Gate 200k coins + lvl 10 (`canEnter`) |
| `RankedHubService` | TP `rankedSpawn` (TierSpace hub) |
| `SafeSpawnService` | Spawn seguro por mundo |
| `WorldModeService` | Classifica mundo SMP/UHC/ranked/lobby |
| `LastLocationTracker` | Última posição antes de modo |

### `combat`

| Classe | Função |
|--------|--------|
| `CombatLogService` | Tag combate, penalidade morte |
| `CombatCommandBlocker` | Bloqueia `/home` etc. em combate |
| `CooldownService` | Cooldown entrada modo |

### `stats` / `leaderboard` / `hologram`

| Classe | Função |
|--------|--------|
| `PlayerStats` | DTO sync backend |
| `PlayerStatsTracker` | Join/quit/coin gain → `/stats/sync` |
| `PlayerStatsSnapshotBuilder` | Monta snapshot de cache + DataStore |
| `LeaderboardService` | Tops locais + remotos |
| `HologramManager` | Atualiza hologramas DecentHolograms |

### `economy` / `globalshop` / `rank` / `daily`

| Classe | Função |
|--------|--------|
| `GlobalEconomyService` | Emeralds network |
| `EmeraldRewardService` | Recompensas por modo |
| `GlobalShopService` / `GlobalShopGui` | Loja Emeralds |
| `RankService` | Ranks network (LuckPerms) |
| `DailyRewardService` | `/daily` |

### `admin` / `bridge`

| Classe | Função |
|--------|--------|
| `AdminModeService` | Vanish, fly, tools |
| `AdminEconomyService` | Grant coins/emeralds staff |
| `AdminAuditLog` | Ficheiro + `POST /log` |
| `InvestigateService` | Seguir jogador |
| `AdminBridgeService` | HTTP minespace-admin (separado Render) |

### `season` / `cosmetic` / `crate` / `battlepass`

| Classe | Função |
|--------|--------|
| `SeasonService` | Temporadas SMP/ranked/hardcore |
| `CosmeticService` | Cosmetics Emeralds |
| `CrateService` | Crates |
| `BattlePassService` | Battle pass GUI |

### `scoreboard` / `papi` / `chat` / `alt`

| Classe | Função |
|--------|--------|
| `ScoreboardManager` | Scoreboard por modo |
| `OBMExpansion` | PlaceholderAPI `%obm_*%` |
| `NetworkChatListener` | Formato chat + anti-spam |
| `AltDetectionService` | Anti-farm mesmo IP |

### Listeners Core (amostra)

`PlayerStorageListener`, `PlayerJoinApiListener`, `StatsListener`, `RespawnListener`, `PortalListener`, `EmeraldEconomyListener`, `BackendKillListener`, `PlayerStateResetListener`.

---

## OBM-SMP (`com.obm.network.smp`) — ~66 classes

| Pacote / classe | Função |
|-----------------|--------|
| `SMPPlugin` | Bootstrap SMP |
| `SMPManager` | Estado mundo SMP |
| `EconomyService` | Coins: balance, deposit, withdraw, pay |
| `ShopService` / `ShopGui` / `ShopListener` | Loja categorias |
| `SellService` / `SellGui` | Venda itens |
| `AuctionService` / `AuctionGui` | Leilão |
| `MarketService` / `MarketGui` | Mercado P2P |
| `LevelService` / `RankService` | Progressão e ranks SMP |
| `PlayerProgressionStore` | Persistência level/rank |
| `SMPListener` | Kills, recompensas |
| `SpawnProtectionListener` | Proteção spawn |
| `commands/*` | `/smp`, `/shop`, `/money`, etc. |

---

## OBM-TierSpace (`com.obm.network.tierspace`) — ~50 classes

| Pacote / classe | Função |
|-----------------|--------|
| `TierSpacePlugin` | Bootstrap ranked |
| `QueueService` | Fila matchmaking |
| `MatchService` / `Match` | Partidas 1v1 |
| `ArenaService` | Arenas configuráveis |
| `RatingCalculator` | ELO / rating |
| `KitService` | Kits por modo |
| `TierSeasonManager` | Season ranked |
| `TierSpaceStore` | YAML stats ranked |
| `anticheat/*` | Spartan/Grim bridges, proteção match |
| `ui/*` | GUIs fila, pós-match, scoreboard TAB |
| `TierSpaceCommand` / `QueueCommand` | `/tierspace`, `/queue` |

---

## OBM-Lobby (`com.obm.network.lobby`) — ~28 classes

| Classe | Função |
|--------|--------|
| `OBMLobbyPlugin` | Bootstrap lobby |
| `JoinHandler` / `QuitHandler` | UX join, item menu |
| `LobbyCommand` | `/lobby`, `/hub` |
| `MenuCommand` | `/menu` |
| `MainMenu` / `MainMenuConfig` | GUI 27 slots |
| `MenuManager` | Clicks → teleporte modos |
| `TopHologramManager` | Hologramas tops (se configurado) |
| `RetentionJoinListener` | Retenção / objectivos hardcore |

---

## OBM-UHC (`com.obm.network.uhc`) — ~12 classes

| Classe | Função |
|--------|--------|
| `OBMUHCPlugin` | Bootstrap Hardcore |
| `UHCManager` | Vidas, estado eliminado |
| `UHCTimer` / `UHCTimeTracker` | Timer partida / playtime |
| `UHCListener` | Morte, vidas |
| `UHCReviveService` | Lógica revive staff |
| `ReviveCommand` / `UHCStatsCommand` | Comandos staff/stats |
| `UHCStatsMenu` | GUI stats |

---

## OBM-Inventory (`com.obm.network.inventory`)

Plugin complementar dependente do Core — inventário por mundo (cache). Consultar `OBMInventoryPlugin` no módulo para detalhe de listeners.

---

## Como navegar

1. Modo de jogo → `WorldModeService` + plugin do modo (SMP/UHC/TierSpace).
2. Economia coins → `PlayerCacheService` → `EconomyManager`.
3. Emeralds → `GlobalEconomyService` (não API coins).
4. Entrada lobby → `NpcModeRouter` ou `MenuManager`.
