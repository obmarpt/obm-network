# Anti-Cheat Setup — MineSpace / TierSpace

## Instalação

1. `GrimAC.jar` → `plugins/`
2. `Spartan.jar` → `plugins/` (opcional; ver `spartan/README.md`)
3. `Vulcan.jar` → `plugins/` (opcional)
4. Copiar `grim/config.yml` e `grim/punishments.yml`
5. Copiar `spartan/checks.yml`, `advanced.yml`, `settings.yml`
6. Aplicar overrides do Vulcan se usado
7. Reiniciar servidor

## Spartan + Grim (falsos positivos movimento)

Ver **`spartan/README.md`**: punishments off, `gravity-simulation`/`speed-simulation` off, Grim trata simulação.

## Grim + Vulcan juntos

| Grim | Vulcan |
|------|--------|
| Reach (3.0+) | KillAura heuristics |
| Knockback / Velocity | AutoClicker |
| Simulation / Timer | Aim (alert only) |
| Hitboxes | Bad Packets / Improbable |
| NoSlow | Scaffold / FastBreak |

## TierSpace — permissoes

- Staff em spectate: `grim.spectator` + Vulcan bypass
- Jogadores em match: nunca `vulcan.bypass.*`

## Falsos positivos

- Primeiras 2 semanas: só alerts (comentar kicks em punishments.yml)
- Ping > 350ms: não punir automaticamente
- TPS < 19.5: checks pausados (minimum-tps)
