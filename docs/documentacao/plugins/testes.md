# Testes — checklist QA completo

Marcar ✅ após validar em servidor de staging. Incluir versão dos JARs e data do teste.

**Pré-condições:** backend Render UP, `plugin-key` correcto, Citizens + LP configurados.

---

## 1. Infraestrutura

| # | Teste | Esperado | ✅ |
|---|--------|----------|---|
| 1.1 | `curl /health` sem key | 200 OK | |
| 1.2 | `GET /player/:uuid` com `X-Plugin-Key` | JSON com balance/coins | |
| 1.3 | Plugins load order | Core → modos → Lobby; zero erros vermelhos no log | |
| 1.4 | Mundos existem | Lobby, world, UHC, rankedSpawn carregados | |

---

## 2. Entrar no servidor

| # | Teste | Esperado | ✅ |
|---|--------|----------|---|
| 2.1 | Primeiro join (UUID novo) | Cria linha em `players`; saldo inicial ≥ 0 | |
| 2.2 | Join repetido | Saldo API aplicado ao jogador (main thread) | |
| 2.3 | Join com API offline + fallback | Usa `data.yml` se configurado | |
| 2.4 | Spawn lobby | Posição `lobby.spawn`; dia fixo se configurado | |
| 2.5 | Item menu / compass | Abre ou indica `/menu` | |
| 2.6 | Scoreboard lobby | Linhas correctas (sem crash) | |
| 2.7 | Quit | Flush cache; sem exception no log | |

---

## 3. Trocar de modos

| # | Teste | Esperado | ✅ |
|---|--------|----------|---|
| 3.1 | NPC Rush | TP `world` (ou primary SMP); som + title | |
| 3.2 | NPC Hardcore sem unlock | Mensagem 200k + lvl 10; sem TP | |
| 3.3 | NPC Hardcore com unlock + vidas | TP UHC | |
| 3.4 | NPC Hardcore 0 vidas | Mensagem sem vidas | |
| 3.5 | NPC TierSpace | TP rankedSpawn | |
| 3.6 | `/menu` → SMP | Igual NPC Rush | |
| 3.7 | `/menu` → Hardcore bloqueado | Igual regra unlock | |
| 3.8 | `/menu` → PvP | TP ranked hub | |
| 3.9 | `/lobby` de mundo SMP | Volta lobby (combat exempt se config) | |
| 3.10 | Duplo clique NPC rápido | Um TP apenas (debounce) | |

---

## 4. Ganhar coins (Rush)

| # | Teste | Esperado | ✅ |
|---|--------|----------|---|
| 4.1 | Kill jogador em SMP | +coins RAM; action-bar se activo | |
| 4.2 | Anti-farm mesmo IP | Sem reward se `alt-detection` activo | |
| 4.3 | `/sell` item válido | +coins | |
| 4.4 | Playtime interval | +coins após intervalo config | |
| 4.5 | Aguardar 30s sync | `POST /money/add` no log backend (delta > 0) | |
| 4.6 | Aguardar 60s stats | `POST /stats/sync` com coins absolutos | |
| 4.7 | Quit após ganhos | DB coins ≈ cliente | |

---

## 5. Gastar coins

| # | Teste | Esperado | ✅ |
|---|--------|----------|---|
| 5.1 | `/shop` compra com saldo | Desconta RAM; item entregue | |
| 5.2 | Compra sem saldo | Mensagem falha | |
| 5.3 | `/money pay` (se permitido) | Transferência entre jogadores | |
| 5.4 | Após gasto + 60s | DB coins desce (stats/sync) | |
| 5.5 | Combat log death penalty | Desconta se configurado | |

---

## 6. Comandos

| # | Comando | Permissão testada | ✅ |
|---|---------|-------------------|---|
| 6.1 | `/lobby` | default true | |
| 6.2 | `/menu` | default true | |
| 6.3 | `/smp` | com e sem `obm.smp.enter` | |
| 6.4 | `/shop`, `/sell`, `/money` | grupo LP SMP | |
| 6.5 | `/queue` TierSpace | `obm.tierspace.queue` | |
| 6.6 | `/tierspace` | menu/info | |
| 6.7 | `/daily`, `/shopglobal` | Emeralds | |
| 6.8 | `/adminpanel` | só staff | |
| 6.9 | `/season smp info` | `obm.season.info` | |
| 6.10 | `/uhcstats` | permissão UHC | |
| 6.11 | `/revive` | `uhc.revive` staff | |

---

## 7. Hardcore — bloqueio e vidas

| # | Teste | Esperado | ✅ |
|---|--------|----------|---|
| 7.1 | Entrada bloqueada &lt; 200k coins | Menu item locked + NPC mensagem | |
| 7.2 | Entrada bloqueada &lt; level 10 | Idem | |
| 7.3 | Morte UHC | Perde vida; stats actualizados | |
| 7.4 | 0 vidas | Não entra Hardcore | |
| 7.5 | Staff `/revive` | Restaura vidas | |
| 7.6 | `hardcore.unlock.enabled: false` | Entrada livre (se vidas &gt; 0) | |

---

## 8. TierSpace

| # | Teste | Esperado | ✅ |
|---|--------|----------|---|
| 8.1 | `/queue <mode>` | Entra fila; feedback chat | |
| 8.2 | Dois jogadores fila | Match inicia; TP arena | |
| 8.3 | Fim match | Rating altera; post-match GUI | |
| 8.4 | Vitória | Emeralds reward ranked (se config) | |
| 8.5 | `/queue leave` | Sai fila | |
| 8.6 | AC exemption durante match | Sem falso positivo óbvio | |

---

## 9. Integração backend

| # | Teste | Esperado | ✅ |
|---|--------|----------|---|
| 9.1 | Request sem header key | 401/403 | |
| 9.2 | Request com key errada | 403 | |
| 9.3 | `POST /money/add` body `{uuid, amount}` | 200 + balance | |
| 9.4 | Body com `coins` em vez de `amount` | 400 `use_amount_not_coins` | |
| 9.5 | `GET /top/coins` | Lista ordenada | |
| 9.6 | `POST /log` após admin action | Entrada na DB/logs | |
| 9.7 | `GET /commands` poll | Comando remoto executa (staging only) | |
| 9.8 | Holograma lobby | Nomes top após sync 60s | |

---

## 10. Emeralds (network)

| # | Teste | Esperado | ✅ |
|---|--------|----------|---|
| 10.1 | `/daily` claim | +emeralds (cooldown IP se activo) | |
| 10.2 | `/shopglobal` compra | -emeralds | |
| 10.3 | PAPI `%obm_emeralds%` | Valor coerente | |
| 10.4 | Leaderboard emeralds | Top local ou remoto conforme config | |

---

## 11. Staff / admin

| # | Teste | Esperado | ✅ |
|---|--------|----------|---|
| 11.1 | `/adminmode` | Vanish/fly | |
| 11.2 | Admin panel grant coins | Jogador vê saldo; audit log | |
| 11.3 | `/investigate` | Segue alvo | |
| 11.4 | Combat + `/home` bloqueado | Mensagem combat log | |
| 11.5 | ALT alert staff | Alerta se 4ª conta mesmo IP | |

---

## 12. Regressões conhecidas (re-test obrigatório)

| # | Área | O que validar | ✅ |
|---|------|---------------|---|
| R1 | NPC nome `RUSH` | Entra Rush, não ignorado | |
| R2 | NPC nome `SMP` | Rush, não Tier | |
| R3 | `/lobby` | Não "Unknown command" | |
| R4 | Saldo após shop | DB não fica inflacionado &gt; 2 min | |
| R5 | Plugin key após rotate | Servidor e Render mesma key | |

---

## Registo de teste (template)

```
Data:
Build: OBM-Core __ / SMP __ / Lobby __
Backend URL:
Tester:
Notas:
```

---

## Automatização futura

- Mock `HttpUtils` em testes unitários `PlayerCacheService`.
- Integration: Testcontainers PostgreSQL + rotas `money.js` / `stats.js`.
- Papermock para `NpcModeRouter.handleClick` com nomes NPC.
