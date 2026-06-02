# MineSpace — Lobby Hologram Pack (DecentHolograms)

## Requisitos

- **DecentHolograms** 2.8+
- **PlaceholderAPI**
- **OBM-Core** + **OBM-TierSpace** (expansion `%obm_*`)

## Config DecentHolograms (obrigatório)

Em `plugins/DecentHolograms/config.yml`:

```yaml
defaults:
  text-format: MINIMESSAGE   # suporta <gradient>, <bold>, <gray>
  update-interval: 20      # hologramas com PAPI (stats)

integrations:
  placeholder-api: true
```

Reinicia após alterar.

## Instalação rápida

### Opção A — Import YAML (recomendado)

1. Copia os ficheiros de `server-config/holograms/yaml/` para:
   `plugins/DecentHolograms/holograms/`
2. Ajusta coordenadas `location` em cada YAML ao teu lobby.
3. `/dh reload`

### Opção B — Comandos in-game

1. Vai ao lobby (`Lobby`)
2. Segue `COMMANDS.md` — posiciona-te em cada zona e corre os comandos.
3. `/dh reload`

## TOP 10 dinâmico (Rating / Streak)

O parser `%obm_top_tierspace_sword_rating_1%` **não funciona** com a expansion actual
(stat key composto `tierspace_sword_rating`).

**Solução:** usa o **TopHologramManager** (OBM-Lobby) nos coords do `config.yml`:

```yaml
holograms:
  tierspace_rating: { world: Lobby, x: 40, y: 65, z: 35 }
  tierspace_streak: { world: Lobby, x: 44, y: 65, z: 35 }
```

Este manager actualiza TOP 10 automaticamente a cada 10s com visual premium.

## Paleta

| Modo | Gradiente |
|------|-----------|
| TierSpace | `#00AAFF` → `#AA00FF` |
| Rush | `#FFAA00` → `#FF7700` |
| Hardcore | `#FF4444` → `#880000` |

## Placeholders úteis

| Placeholder | Uso |
|-------------|-----|
| `%obm_coins%` | Coins Rush |
| `%obm_level%` | Level |
| `%obm_rank%` | Rank Rush |
| `%obm_tier_rating_sword%` | Rating TierSpace |
| `%obm_tier_rank_sword%` | Rank Tier |
| `%obm_tier_wins_sword%` | Wins |
| `%obm_tier_streak_sword%` | Streak actual |
| `%obm_tier_best_streak_sword%` | Best streak |
| `%obm_tier_season%` | Season |
| `%obm_top_smp_kills_1%` | #1 nome (Rush kills) |
| `%obm_top_smp_kills_1_value%` | #1 valor |
