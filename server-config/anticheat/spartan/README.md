# Spartan + GrimAC — MineSpace

## Conflito (causa dos falsos positivos)

| Área | Spartan | GrimAC |
|------|---------|--------|
| Queda / gravidade | `gravity-simulation` | `Simulation`, `NoFall` |
| Velocidade | `speed-simulation` | `Simulation`, `Timer` |
| Knockback | `velocity` | `Knockback`, `Explosion` |
| Reach | `hit-reach`, `block-reach` | `Reach` |

**Dois anticheats a simular movimento ao mesmo tempo** geram flags em jogadores legítimos (lobby, escadas, blocos parciais, ping, 1.21).

## Configuração aplicada (recomendada)

1. **Spartan `checks.yml`**
   - Punishments **desactivados** em todos os checks (`enabled: false`, sem `spartan kick`).
   - `gravity-simulation` e `speed-simulation` **desactivados** (Java + Bedrock) — Grim cobre movimento.
   - `detection_details` mantém **logs** em `plugins/Spartan/logs/`.

2. **Spartan `advanced.yml`**
   - `minimum_gravity_difference: 0.12` (antes `0.0` = máxima sensibilidade).
   - `minimum_speed_difference: 0.05`.

3. **Spartan `settings.yml`**
   - `ground_teleport_on_detection: false` (evita rubber-band / FP).

4. **GrimAC `punishments.yml`**
   - Movimento: só `[alert]` + `[log]` (sem kick automático na fase actual).

## Deploy

Copiar para o servidor:

```
server-config/anticheat/spartan/checks.yml     → plugins/Spartan/checks.yml
server-config/anticheat/spartan/advanced.yml   → plugins/Spartan/advanced.yml
server-config/anticheat/spartan/settings.yml   → plugins/Spartan/settings.yml
```

Depois: `/spartan reload` ou reinício.

Staff com notificações: `/spartan notifications` (ver flags sem kick).

## Alternativa: só Grim

- Remover `Spartan.jar` do `plugins/`.
- Manter Grim para movimento, reach, timer, knockback.
- Spartan útil para: kill-aura heuristics, fast-clicks, x-ray — se precisares, mantém Spartan **sem** checks de simulação.

## Confirmação

Com esta config, **Spartan não executa `spartan kick`** em nenhum check. `gravity-simulation` não corre em Java — o kick `gravity-simulation` deixa de ocorrer.
