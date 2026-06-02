# Comandos DecentHolograms — copiar e colar in-game

> Posiciona-te em cada local antes de `/dh create`.  
> Depois usa `/dh movehere <nome>` se precisares ajustar.

---

## 0. SERVER INFO / WELCOME

**Local sugerido:** spawn lobby, 3 blocos à frente, y+4  
`Lobby` → `0.5 168.0 -6.5`

```
/dh create obm_welcome
/dh line add obm_welcome <gradient:#00AAFF:#AA00FF><bold>✦ MineSpace ✦</bold></gradient>
/dh line add obm_welcome <dark_gray>──────────────</dark_gray>
/dh line add obm_welcome <gray>Bem-vindo, </gray><white>%player_name%</white>
/dh line add obm_welcome <gray>Escolhe o teu modo abaixo</gray>
/dh line add obm_welcome <yellow>⚔ Rush</yellow> <gray>·</gray> <red>💀 Hardcore</red> <gray>·</gray> <aqua>🏆 TierSpace</aqua>
/dh line add obm_welcome <dark_gray>──────────────</dark_gray>
/dh line add obm_welcome <gradient:#FFAA00:#FF7700>🎯 Domina o ranking!</gradient>
/dh setupdateinterval obm_welcome 40
```

---

## 1. TIERSPACE LEADERBOARD (TOP 10 RATING)

> **Usa TopHologramManager** em `x:40 y:65 z:35` (config OBM-Lobby).  
> Se quiseres criar manualmente com PAPI parcial (top 3 + resto via manager):

**Local:** `Lobby` → `40.5 66.0 35.5`

```
/dh create obm_tierspace_top
/dh line add obm_tierspace_top <gradient:#00AAFF:#AA00FF><bold>🏆 TIER TOP — RATING</bold></gradient>
/dh line add obm_tierspace_top <dark_gray>──────────────</dark_gray>
/dh line add obm_tierspace_top <gold><bold>#1</bold></gold> <white>%obm_top_smp_kills_1%</white> <gray>—</gray> <yellow>%obm_top_smp_kills_1_value%</yellow>
/dh line add obm_tierspace_top <gray>#2</gray> <white>%obm_top_smp_kills_2%</white> <gray>—</gray> <yellow>%obm_top_smp_kills_2_value%</yellow>
/dh line add obm_tierspace_top <gray>#3</gray> <white>%obm_top_smp_kills_3%</white> <gray>—</gray> <yellow>%obm_top_smp_kills_3_value%</yellow>
/dh line add obm_tierspace_top <dark_gray>──────────────</dark_gray>
/dh line add obm_tierspace_top <gradient:#00AAFF:#AA00FF>⚡ Sobe no ranking!</gradient>
/dh setupdateinterval obm_tierspace_top 60
```

> ⚠️ Para **TOP 10 TierSpace rating real**, mantém `TopHologramManager` activo — ver README.

---

## 2. TIER STREAK LEADERBOARD (TOP 10)

**Local:** `Lobby` → `44.5 66.0 35.5` (TopHologramManager)

Comandos manuais (alternativa):

```
/dh create obm_tierspace_streak
/dh line add obm_tierspace_streak <gradient:#AA00FF:#FF4444><bold>🔥 TOP STREAK</bold></gradient>
/dh line add obm_tierspace_streak <dark_gray>──────────────</dark_gray>
/dh line add obm_tierspace_streak <gold><bold>#1</bold></gold> <white>—</white> <yellow>—</yellow>
/dh line add obm_tierspace_streak <gray>#2</gray> <white>—</white> <yellow>—</yellow>
/dh line add obm_tierspace_streak <gray>#3</gray> <white>—</white> <yellow>—</yellow>
/dh line add obm_tierspace_streak <dark_gray>──────────────</dark_gray>
/dh line add obm_tierspace_streak <red>🔥 Mantém a streak!</red>
/dh setupdateinterval obm_tierspace_streak 60
```

> Recomendado: **TopHologramManager** preenche linhas automaticamente.

---

## 3. RUSH INFO HOLOGRAM

**Local sugerido:** portal Rush, esquerda  
`Lobby` → `-8.5 166.0 4.5`

```
/dh create obm_rush_info
/dh line add obm_rush_info <gradient:#FFAA00:#FF7700><bold>⚔ RUSH SMP</bold></gradient>
/dh line add obm_rush_info <dark_gray>──────────────</dark_gray>
/dh line add obm_rush_info <gray>Farm → Vender → Gear → PvP</gray>
/dh line add obm_rush_info <gray>Mundo:</gray> <white>world</white> <gray>+ nether/end</gray>
/dh line add obm_rush_info <gray>Economia:</gray> <yellow>💰 Activa</yellow>
/dh line add obm_rush_info <dark_gray>──────────────</dark_gray>
/dh line add obm_rush_info <gradient:#FFAA00:#FF7700><bold>▶ Entra pelo menu!</bold></gradient>
```

---

## 4. HARDCORE INFO HOLOGRAM

**Local sugerido:** portal Hardcore, direita  
`Lobby` → `8.5 166.0 4.5`

```
/dh create obm_hardcore_info
/dh line add obm_hardcore_info <gradient:#FF4444:#880000><bold>💀 HARDCORE</bold></gradient>
/dh line add obm_hardcore_info <dark_gray>──────────────</dark_gray>
/dh line add obm_hardcore_info <gray>Sobrevive. Evolui. Morre.</gray>
/dh line add obm_hardcore_info <gray>Mundo:</gray> <white>uhc</white> <gray>+ nether/end</gray>
/dh line add obm_hardcore_info <red>⚠ Vidas limitadas</red>
/dh line add obm_hardcore_info <dark_gray>──────────────</dark_gray>
/dh line add obm_hardcore_info <gradient:#FF4444:#880000><bold>▶ Entra pelo menu!</bold></gradient>
```

---

## 5. STATS HOLOGRAM (personalizado por jogador)

**Local sugerido:** junto ao spawn  
`Lobby` → `-3.5 165.5 0.5`

```
/dh create obm_player_stats
/dh line add obm_player_stats <gradient:#FFD700:#FFAA00><bold>📊 AS TUAS STATS</bold></gradient>
/dh line add obm_player_stats <dark_gray>──────────────</dark_gray>
/dh line add obm_player_stats <gray>💰 Coins:</gray> <yellow>%obm_coins%</yellow>
/dh line add obm_player_stats <gray>📈 Level:</gray> <white>%obm_level%</white> <gray>| Rank:</gray> <yellow>%obm_rank%</yellow>
/dh line add obm_player_stats <gray>🏆 Tier:</gray> <aqua>%obm_tier_rating_sword%</aqua> <gray>|</gray> <white>%obm_tier_rank_sword%</white>
/dh line add obm_player_stats <dark_gray>──────────────</dark_gray>
/dh line add obm_player_stats <gray>W/L:</gray> <green>%obm_tier_wins_sword%</green><gray>/</gray><red>%obm_tier_losses_sword%</red>
/dh setupdateinterval obm_player_stats 20
/dh enableupdate obm_player_stats
```

---

## 6. QUEUE / TIER ENTRY HOLOGRAM

**Local sugerido:** zona TierSpace / NPC  
`Lobby` → `0.5 166.0 8.5`

```
/dh create obm_tierspace_entry
/dh line add obm_tierspace_entry <gradient:#00AAFF:#AA00FF><bold>🏆 TIERSPACE</bold></gradient>
/dh line add obm_tierspace_entry <dark_gray>──────────────</dark_gray>
/dh line add obm_tierspace_entry <gray>PvP competitivo 1v1 · Elo ranking</gray>
/dh line add obm_tierspace_entry <gray>Season:</gray> <white>%obm_tier_season%</white> <gray>(</gray><aqua>%obm_tier_season_days%</aqua><gray>d restantes)</gray>
/dh line add obm_tierspace_entry <dark_gray>──────────────</dark_gray>
/dh line add obm_tierspace_entry <gradient:#00AAFF:#AA00FF><bold>🎯 Click to play</bold></gradient>
/dh line add obm_tierspace_entry <aqua>⚡ Queue now — abre o menu!</aqua>
/dh setupdateinterval obm_tierspace_entry 40
```

---

## Comandos úteis

```
/dh list                          # listar hologramas
/dh movehere <nome>               # mover para a tua posição
/dh edit <nome>                   # editor visual (se disponível)
/dh line remove <nome> <index>    # remover linha
/dh delete <nome>                 # apagar
/dh reload                        # recarregar
```

## Notas

- Hologramas com `%obm_*%` precisam de **update interval** (20–60 ticks).
- TOP 10 TierSpace rating/streak: usa **TopHologramManager** (OBM-Lobby) — já integrado com DataStore.
- Cores MiniMessage requerem `text-format: MINIMESSAGE` no config DH.
