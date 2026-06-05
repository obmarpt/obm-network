# Problemas, bugs e riscos

Análise read-only do código (estado actual do repositório). Severidade: **Alta** / **Média** / **Baixa**.

---

## Economia e backend

### Alta — gastos não enviam delta negativo via `POST /money/add`

- `PlayerData.removeCoins` marca `dirty` mas **não** decrementa `pendingCoinDelta`.
- API `money/add` só aceita `amount > 0` (incremento).
- **Mitigação parcial:** `POST /stats/sync` (60s / quit) sobrescreve `coins` com saldo absoluto do cache.
- **Risco:** janela de 30–60s em que PostgreSQL pode mostrar saldo **maior** que o real se só `money/add` corre antes de `stats/sync`.

### Alta — `backend.plugin-key` default `change-me-plugin-key`

- Sem chave igual a `PLUGIN_API_KEY` no Render → **403** em todas as requests.
- Sintoma: jogadores com 0 coins após join, tops vazios, logs não chegam.

### Média — Emeralds não migrados para API

- Coins Rush → PostgreSQL + cache.
- Emeralds → `GlobalEconomyService` + `data.yml`.
- Dashboard que só lê API não vê Emeralds correctamente.

### Média — dupla fonte de verdade offline

- Com backend ON: join carrega API; fallback `data.yml` se timeout.
- Editar manualmente `data.yml` com jogador online → desync até quit/stats sync.

### Baixa — campo `coins` rejeitado no body

- Backend devolve 400 se plugin enviar `coins` em vez de `amount` (corrigido no `EconomyManager`).

---

## Modos e NPCs

### Média — ordem de matching no nome NPC

- Hardcore antes de Tier antes de Rush — **intencional**.
- NPC com nome `"SMP Hardcore"` → Hardcore (contém `hardcore`).
- NPC só `"SMP"` → Rush (alias), não menu SMP antigo.

### Média — NPCs ignorados se nome não reconhecido

- Sem match → log `fine` apenas; jogador não recebe mensagem clara (config `npc.debug: true` ajuda ops).

### Média — menu PvP vs TierSpace

- Slot PvP no `MainMenu` usa `RankedHubService` (TierSpace hub), **não** fila ranked.
- Fila ranked exige `/queue` + permissão `obm.tierspace.queue`.

### Baixa — debounce NPC 400ms

- Duplo clique rápido pode ignorar segundo clique (comportamento esperado anti-duplo TP).

### Hardcore — regras de entrada

- Unlock: `hardcore.unlock` (default 200k coins + level 10 via `SMPBridge`).
- Vidas `lives_uhc` em DataStore — 0 vidas bloqueia mesmo com unlock OK.
- Menu e NPC **ambos** verificam unlock; consistente.

---

## Permissões

### Alta — `obm.smp.*` e `obm.tierspace.*` default **false**

- `/smp`, `/shop`, `/money`, `/queue` → "sem permissão" para jogadores sem LuckPerms.
- **NPC e menu principal NÃO checam** `obm.smp.enter` — entrada por GUI/NPC funciona sem LP.

### Média — inconsistência UX

- Jogador entra Rush por NPC mas `/shop` falha → confusão; documentar grupos LP recomendados.

### Baixa — UHC stats `obm.uhc.stats` default false

- `/uhcstats` só com permissão explícita.

---

## Comandos e integrações

### Média — Essentials vs combat log

- `combatlog.blocked-commands` inclui `home`, `warp`, `teleport` — pode colidir com aliases Essentials.

### Média — comandos remotos `GET /commands`

- Executam na **consola** do servidor — risco operacional se API comprometida (mitigar com API key forte).

### Baixa — `/lobby` vs combat

- Config `lobby.force-always` e mundos lobby exempt — verificar `config.yml` live.

---

## Sincronização e dashboard

### Alta — dois backends HTTP

| Sistema | Auth | Dados |
|---------|------|-------|
| `obm-network-backend` (Render) | `X-Plugin-Key` | coins, stats, tops, logs |
| `minespace-admin` bridge | `X-Bridge-Key` | audit staff, heartbeat |

- Painel web admin **não** substitui API de jogadores; dashboard unificado requer consumir Render ou replicar dados.

### Média — leaderboards remotos vs locais

- Hologramas: cache API 60s.
- `/top` SMP: DataStore local — podem divergir da API.

### Média — kills/playtime na DB

- Dependem de `StatsListener` + chaves `PlayerStatsKeys` + sync; UHC kills em chave `kills_uhc` separada.

---

## Estabilidade / gameplay

### Média — `location_restore_locked_*`

- Impede restore de posição ao mudar modo; se flag ficar presa, comportamento estranho ao voltar (raro).

### Média — reset estado ao entrar modo

- `NpcModeRouter` cura, remove efeitos, full food — pode interferir com combate se NPC clicado em fight.

### Baixa — inventário global

- `GlobalStorageListener` + OBM-Inventory — testar troca mundo com itens diferentes.

### Baixa — anti-cheat TierSpace

- Dependência Spartan/Grim — sem plugin, bridges no-op.

---

## Build / repo

### Baixa — ficheiros `target/` untracked no git status

- Não commitar `target/`; usar `mvn package` limpo em CI.

### Baixa — duplicados path Windows (`OBM-Lobby\` vs `/`)

- Possível confusão em IDE; não afecta runtime se um único source.

---

## Resumo acções imediatas

1. Configurar `backend.plugin-key` = `PLUGIN_API_KEY` no Render.
2. LuckPerms: grupo default com `obm.smp.shop`, `obm.smp.money`, etc., se comandos SMP devem funcionar.
3. Nomear NPCs Citizens: `Rush`, `Hardcore`, `TierSpace` (substring match).
4. Validar `stats/sync` activo para corrigir saldo após compras na loja.
5. Não depender só de `money/add` para saldo final — confirmar `PlayerStatsTracker` no enable.
