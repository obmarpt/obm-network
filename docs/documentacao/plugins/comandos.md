# Comandos — lista completa (plugins OBM)

Permissões `default` vêm do `plugin.yml` de cada módulo. Em produção, **LuckPerms** define o que jogadores recebem.

---

## OBM-Core

| Comando | Aliases | Permissão | Descrição / efeito |
|---------|---------|-----------|-------------------|
| `/admin` | — | `obm.admin.panel` (op) | Hub staff — abre fluxo admin |
| `/adminpanel` | — | `obm.admin.panel` | Painel GUI admin; `setseason` |
| `/adminmode` | — | `obm.admin.mode` | Vanish, fly, ferramentas staff |
| `/investigate` | — | `obm.admin.investigate` | Seguir/investigar jogador |
| `/adminlogs` | — | `obm.admin.logs` | Últimas linhas audit log |
| `/shopglobal` | gshop, globalloja | `obm.shop.global` | Loja global Emeralds |
| `/daily` | — | `obm.daily` | Recompensa diária Emeralds |
| `/cosmetics` | cosmeticos, cos | `obm.cosmetics` | Menu cosmetics |
| `/crate` | crates, caixa | `obm.crate` | Abrir crates |
| `/battlepass` | bp, passe | `obm.battlepass` | Battle Pass GUI |
| `/season` | — | `obm.season.info` | Info/gerir seasons por modo |

**`/season` subcomandos (admin `obm.admin.season`):**

- `smp info|start|end|reset|top <stat>`
- `ranked info|start|end|reset`
- `hardcore info` (sem reset)

---

## OBM-Lobby

| Comando | Aliases | Permissão | Descrição / efeito |
|---------|---------|-----------|-------------------|
| `/lobby` | hub, l | `obm.lobby` (true) | Teleporta spawn lobby; ignora combat se `force-always` |
| `/menu` | — | `obm.menu` (true) | Abre menu principal (27 slots) |

---

## OBM-SMP

| Comando | Aliases | Permissão | Descrição / efeito |
|---------|---------|-----------|-------------------|
| `/smp` | — | `obm.smp.enter` (false) | Entrar mundo Rush SMP |
| `/shop` | — | `obm.smp.shop` (false) | Loja SMP (categorias) |
| `/sell` | — | `obm.smp.sell` (false) | Vender itens (GUI) |
| `/ah` | — | `obm.smp.auction` (false) | Leilão |
| `/auction` | — | `obm.smp.auction` | Idem `/ah` |
| `/money` | — | `obm.smp.money` (false) | Ver saldo; `pay <jogador> <valor>` |
| `/market` | — | `obm.smp.market` (false) | Mercado P2P: list, sell, buy, remove |
| `/rank` | — | `obm.smp.rank` (false) | Ver/comprar ranks SMP |
| `/top` | — | `obm.smp.top` (false) | Leaderboards locais (DataStore) |
| `/stats` | — | `obm.stats` (op) | Staff: stats de outro jogador |

---

## OBM-TierSpace

| Comando | Aliases | Permissão | Descrição / efeito |
|---------|---------|-----------|-------------------|
| `/tierspace` | ts, tier | `obm.tierspace.use` (false) | Menu/info/stats/season |
| `/queue` | — | `obm.tierspace.queue` (false) | Fila ranked: mode, leave, status |

---

## OBM-UHC

| Comando | Permissão | Descrição / efeito |
|---------|-----------|-------------------|
| `/uhcstats` | `obm.uhc.stats` (false) | Stats UHC do jogador |
| `/revive` | `uhc.revive` | Revive jogador (2 vidas) |
| `/revivegeral` | `uhc.revive` | Revive todos eliminados |
| `/revivetotal` | `uhc.revive` | Revive + itens + TP spawn UHC |

---

## Comandos remotos (backend)

- `GET /commands` → Core executa na consola (`BackendService.pollRemoteCommandsAsync`).
- Não são comandos Bukkit registados — vêm da API.

---

## Permissões staff Core (referência)

| Permissão | Uso |
|-----------|-----|
| `obm.admin.economy.coins` | GUI dar/remover coins |
| `obm.admin.economy.emeralds` | GUI Emeralds |
| `obm.admin.economy.unlimited` | Sem limites grant |
| `obm.staff.alt.alert` | Alertas ALT |
| `obm.chat.staff` | Chat staff + bypass spam |
| `obm.rank.owner/admin/mod/helper/builder` | Prefixos network |

---

## Notas de produção

1. Jogadores **default** não têm `obm.smp.*` — precisam grupo LP ou entram só via **NPC/menu** (sem check de enter).
2. `/lobby` e `/menu` são `default: true` — funcionam para todos.
3. Essentials pode registar `/home`, `/warp` — combat log do Core pode bloquear subset.
