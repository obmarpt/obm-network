# Modos de jogo

Três modos principais + **Lobby** como hub. Classificação de mundos: `WorldModeService` (OBM-Core).

---

## Lobby (hub)

| Aspeto | Detalhe |
|--------|---------|
| Mundo | `lobby.world` (ex.: `Lobby`) |
| Plugin | OBM-Lobby + Core (spawn, hologramas, NPCs) |
| Entrada | Join do servidor, `/lobby`, `/hub` |
| Saída | Menu GUI, NPC Citizens, portais (`PortalListener`) |

**Funcionalidades:** menu 27 slots (Rush / Hardcore / PvP), hologramas DecentHolograms, estatísticas PAPI, retenção hardcore.

---

## 1. Rush SMP

### Lógica

- Economia **coins** (shop, sell, auction, market, pay).
- Progression SMP: level, XP, ranks compráveis (`RankService`).
- Anti-farm: `KillFarmGuard`, cooldowns kill, ALT detection (Core).
- Spawn protection, sell catalog gerado (`sell-catalog.yml`).

### Regras principais

- Mundos: lista `smp-worlds` no Core.
- Permissão entrada comando: `obm.smp.enter` (**default: false** em plugin.yml).
- NPC/menu: **não verifica** `obm.smp.enter` — teleporte directo.
- Combat log no Core pode bloquear alguns comandos; `/lobby` com `force-always` ignora.

### Entrada

| Método | Implementação |
|--------|----------------|
| NPC Citizens | `NpcModeRouter.joinRush()` → mundo `getPrimarySMPWorld()` |
| Menu lobby | `MenuManager` → slot SMP → teleporte |
| Comando | `/smp` → `SMPCommand` + `SmpPermissions.ENTER` |

### Integração backend

- Join: `POST /playerJoin`, `GET /player/:uuid` → cache coins.
- Gameplay: só RAM (`PlayerData.addCoins`).
- Sync: `POST /money/add` (deltas), `POST /stats/sync` (snapshot).
- Emeralds network: recompensas separadas (`EmeraldRewardService`).

---

## 2. Hardcore (UHC)

### Lógica

- Vidas limitadas (`lives_uhc` no DataStore).
- Unlock gate: level SMP ≥ 10 **e** coins ≥ 200k (`HardcoreUnlockService`).
- Morte: stats `kills_uhc`, `deaths_uhc`, playtime `playtime_uhc`.
- OBM-UHC: timer, actionbar, revive staff, mundo hardcore auto-load.

### Regras principais

- **Sem season reset** — dados permanentes.
- Entrada bloqueada se unlock falhar ou vidas ≤ 0.
- Config: `hardcore.unlock` em `OBM-Core/config.yml`.

### Entrada

| Método | Implementação |
|--------|----------------|
| NPC | `NpcModeRouter.handleHardcore()` — unlock + vidas |
| Menu lobby | Slot Hardcore → validação igual |
| UHC plugin | Jogador já no mundo UHC por teleporte |

### Integração backend

- Stats kills/playtime no `POST /stats/sync`.
- Coins no join/sync (mesmo pipeline Rush).
- Vidas **não** na API — só `data.yml`.

---

## 3. TierSpace (Ranked PvP)

### Lógica

- ELO/rating por modo (ex.: sword) — `TierSpaceBridge`, DataStore `tierspace_*`.
- Filas competitivas (`/queue`), arenas, kits.
- Seasons ranked: `TierSeasonManager`, `/season ranked`.
- Integração Grim/Spartan (softdepend).

### Regras principais

- Hub: `rankedSpawn` (configurável).
- Permissões: `obm.tierspace.use`, `obm.tierspace.queue` (**default: false**).
- NPC teleporta para hub — **não inicia fila** automaticamente.

### Entrada

| Método | Implementação |
|--------|----------------|
| NPC | `NpcModeRouter.joinTierSpace()` → `RankedHubService.teleport()` |
| Menu | Slot PvP → hub TierSpace |
| Comandos | `/tierspace`, `/queue` |

### Integração backend

- Rating/stats locais em `data.yml`.
- Leaderboard hologramas: `tierspace_sword_rating` (DataStore).
- Tops globais API: separados (coins/kills não são rating).

---

## Comparativo rápido

| | Rush SMP | Hardcore | TierSpace |
|---|----------|----------|-----------|
| Moeda principal | Coins | — (unlock usa coins SMP) | ELO |
| Moeda network | Emeralds | Emeralds | Emeralds |
| Backend coins | Sim (cache) | Sim (read) | N/A |
| Season reset | Sim (SMP) | Não | Sim (ranked) |
| Plugin dedicado | OBM-SMP | OBM-UHC | OBM-TierSpace |
