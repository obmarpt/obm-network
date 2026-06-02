# Anti-Cheat Setup — MineSpace / TierSpace

## Instalação

1. `GrimAC.jar` → `plugins/`
2. `Vulcan.jar` → `plugins/`
3. Copiar `grim/config.yml` e `grim/punishments.yml`
4. Aplicar overrides do Vulcan (secção `connection.other-anticheat-support: true`)
5. Reiniciar servidor
6. Executar comandos `/vulcan disablecheck` listados no overrides

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
