# Hologramas MineSpace

## Onde está tudo

| Origem | Ficheiro / classe |
|--------|-------------------|
| **Config central** | `plugins/OBM-Core/holograms.yml` |
| **API** | `OBM-Core` → `com.obm.network.core.hologram.HologramService` |
| **Lobby** | `OBMLobbyPlugin` só chama `getHologramService().reload()` |
| **Estáticos antigos** | `plugins/DecentHolograms/holograms/obm_*.yml` — desactivar se migraste para `holograms.yml` |

## Comandos úteis

- `/dh reload` — DecentHolograms
- Reiniciar servidor ou recarregar OBM-Core + OBM-Lobby após editar `holograms.yml`

## Regras

- Cores legacy `&` apenas (sem `<gradient>`)
- Sem linhas vazias (o serviço substitui por `&7·`)
