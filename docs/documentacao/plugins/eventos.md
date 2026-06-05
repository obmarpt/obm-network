# Eventos importantes

## PlayerJoinEvent

| Listener | Plugin | Prioridade | Acção |
|----------|--------|------------|-------|
| `PlayerStorageListener` | Core | MONITOR | `loadPlayerAsync` — API + cache |
| `PlayerJoinApiListener` | Core | MONITOR | `POST /playerJoin` se storage off |
| `PlayerStatsTracker` | Core | MONITOR | Log `PLAYER_JOIN`, debounce stats sync |
| `JoinHandler` | Lobby | — | UX lobby, compass, mensagens |
| `PlayerStateResetListener` | Core | — | Estabilização spawn |
| `StatsListener` | Core/SMP | — | Init chaves stats |
| `EmeraldEconomyListener` | Core | — | Warm emeralds |
| `ScoreboardListener` | Core | — | Scoreboard por modo |
| `RetentionJoinListener` | Lobby | — | Objectivos hardcore |
| Vários | Cosmetics, BP, Alt | — | Warm caches |

**Threading join backend:** HTTP em **async**; aplicar coins no **main** após fetch.

---

## PlayerQuitEvent

| Listener | Acção |
|----------|-------|
| `PlayerStorageListener` | Flush coins + remove cache |
| `PlayerStatsTracker` | Log quit + `POST /stats/sync` |
| `StatsListener` | `dataStore.save(uuid)` |
| `GlobalStorageListener` | Guardar inventário/posição |
| `CombatLogService` | Limpar combat tag |
| `LastLocationTracker` | Última posição |

---

## Economia (gameplay)

| Evento / trigger | Onde | Efeito |
|------------------|------|--------|
| Kill jogador SMP | `SMPListener` / Core rewards | +coins, stats, `BackendStorageHook` |
| Sell item | `SellService` | +coins cache |
| Shop buy | `ShopService` | -coins cache |
| Playtime tick | `TimeTracker` | +playtime_smp/uhc, emeralds interval |
| Global emerald grant | `EmeraldRewardService` | +emeralds DataStore |
| Admin GUI grant | `AdminEconomyService` | coins/emeralds + audit |

**HTTP:** apenas deltas acumulados (`pendingCoinDelta`) → sync 30s ou quit.

---

## NPC interaction

| Evento | Classe | Lógica |
|--------|--------|--------|
| `NPCRightClickEvent` | `NPCClickListener` | Citizens → `NpcModeRouter` |
| `PlayerInteractEntityEvent` | `NPCClickListener` | Fallback Citizens NPC |

**Ordem detecção nome (lowercase, contains):**

1. hardcore / uhc / 💀  
2. tier / tierspace / ranked / 🏆  
3. rush / ⚔  
4. smp / 💎 → Rush  

Debounce 400ms anti-duplo teleporte.

---

## Comandos admin

| Acção | Audit |
|-------|-------|
| GUI admin panel | `AdminAuditLog` → ficheiro + `POST /log` |
| Investigate / adminmode | Listeners + logs |
| Season start/end | `SeasonCommand` + audit |
| Remote command | `BackendService` log `REMOTE_COMMAND` |

---

## Outros eventos relevantes

| Evento | Plugin | Notas |
|--------|--------|-------|
| `PlayerDeathEvent` | Core Stats, UHC | kills/deaths, vidas UHC |
| `EntityDeathEvent` | StatsListener | kills UHC/SMP |
| `PlayerChangedWorldEvent` | JoinHandler, scoreboard | Troca modo visual |
| `InventoryClickEvent` | MenuManager, GUIs | Menus lobby/SMP/admin |
| `AsyncPlayerChatEvent` | NetworkChatListener | Formato + anti-spam |

---

## Schedulers (não eventos Bukkit)

| Task | Intervalo | Função |
|------|-----------|--------|
| `PlayerCacheSyncTask` | 30s | `POST /money/add` |
| `PlayerStatsTracker` | 60s | `POST /stats/sync` online |
| `LeaderboardService` local | 5 min | Cache tops YAML |
| `LeaderboardService` remote | 60s | `GET /top/*` |
| `BackendService` poll | 5s | `GET /commands` |
| `DataStore.save` | 5 min | Persistência YAML |
