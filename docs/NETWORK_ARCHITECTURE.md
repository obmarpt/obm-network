# OMD Network — Arquitectura e Seasons

## Modos e seasons

| Modo | Season | Reset |
|------|--------|-------|
| **TierSpace (Ranked)** | `TierSeasonManager` + `tierspace.season.*` | ELO suave + placement (`/season ranked start`) |
| **RushSMP** | `smp.season.*` no Core | coins, level, stats SMP — **não** Emeralds/cosmetics/ranks network |
| **Hardcore** | ❌ Nenhuma | Dados permanentes (protecção no código) |

**Não existe reset global.** `SeasonService` orquestra por `SeasonMode` (SMP, RANKED, HARDCORE).

## Comandos

```
/season                          — resumo dos 3 modos
/season smp info|start [force]|end|reset|top <stat>
/season ranked info|start|end|reset
/season hardcore info            — sempre bloqueado para acções
/tierspace season advance        — equivalente ranked (TierSpace)
```

Admin: `obm.admin.season`

## Config

`plugins/OBM-Core/season.yml` → secção `season.modes.{smp,ranked,hardcore}`

## PAPI

- `%obm_tier_season%` — ranked (TierSpace)
- `%obm_season_smp%`, `%obm_season_smp_days%` — RushSMP

## Sistemas reutilizados

- **Ranked:** `TierSpaceAdminBridge` → `TierSeasonManager.startNewSeason / endSeason / resetAllRatings`
- **SMP:** `AdminResetService.resetRushStats/Economy` + `resetSmpProgression`
- **Hardcore:** `SeasonService` retorna imediatamente sem alterar dados

## Daily / ranks / economias

Ver secções anteriores do doc (daily unificado no Core, ranks network vs SMP separados).
