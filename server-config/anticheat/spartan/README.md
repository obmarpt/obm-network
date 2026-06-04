# Spartan — OMD Network (movimento / false positives)

## Problema (logs)

`gravity-simulation` com `Punish: true` e `Punishment-Level: 40` expulsava jogadores legítimos (saltos, água, wind charge, knockback). O valor `minimum_gravity_difference: 0.0` no `advanced.yml` era sensibilidade máxima.

## Filosofia aplicada

| Objetivo | Config Spartan |
|----------|----------------|
| Check activo | `enabled.java: true` |
| Corrigir movimento | `cancelled_event: true` |
| Sem kick | `punishments.enabled: false` + comandos vazios |
| Menos FP | `minimum_gravity_difference` / `minimum_speed_difference` elevados |
| Sem rubber-band extra | `ground_teleport_on_detection: false` |

## 1. gravity-simulation (principal)

```yaml
# checks.yml
cancelled_event: true      # cancel: true
punishments.enabled: false # punish: false
enabled.java: true
commands: (todos vazios)

# advanced.yml
minimum_gravity_difference: 0.15
```

**Kick eliminado:** com `punishments.enabled: false`, o Spartan deixa de aplicar punição automática ao nível 40 (`Punish: false` nos logs). Detecções continuam; movimento inválido é **cancelado**, não expulso.

## 2. Outros checks de movimento

| Check | Papel | checks.yml |
|-------|--------|------------|
| `speed-simulation` | speed / sprint anómalo | `cancelled_event: true`, punish off, enabled |
| `velocity` | knockback, explosões, wind charge | `cancelled_event: true`, punish off |
| `irregular-movements` | fly-like / movimento irregular | `cancelled_event: true`, punish off |

Todos os restantes checks: **punish desactivado** (sem `spartan kick` em nenhum nível 1–10).

## 3. Compatibilidade gameplay

- **Água / slime:** tolerância via `minimum_gravity_difference: 0.15` e cancel em vez de kick.
- **Wind charge / velocity PvP:** `velocity` com cancel, sem punish.
- **GrimAC:** evitar kicks duplos em movimento — ver `server-config/anticheat/grim/punishments.yml` (alert/log apenas na fase actual).

## 4. Deploy

```
server-config/anticheat/spartan/checks.yml   → plugins/Spartan/checks.yml
server-config/anticheat/spartan/advanced.yml → plugins/Spartan/advanced.yml
server-config/anticheat/spartan/settings.yml → plugins/Spartan/settings.yml
```

Depois: `/spartan reload` ou reinício do servidor.

## 5. Teste in-game

1. Saltar em loop + entrar em água + usar wind charge.
2. Verificar: **sem kick**; possível correção suave de posição (cancel).
3. Logs: `(Punish: false)` em `plugins/Spartan/logs/`.
4. Staff: `/spartan notifications` para ver flags.

## 6. Se ainda houver FP

- Subir `minimum_gravity_difference` para `0.18`–`0.22`.
- Subir `minimum_speed_difference` para `0.10`–`0.12`.
- Desactivar só `gravity-simulation` ou `speed-simulation` no Spartan e deixar Grim tratar movimento (último recurso).
- Não dar `spartan.bypass` a jogadores normais; só OWNER se necessário.
