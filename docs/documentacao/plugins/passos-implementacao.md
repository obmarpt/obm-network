# Passos de implementação (guia operacional)

Ordem recomendada para colocar o servidor **funcional de ponta a ponta**. Não altera código — checklist de ops + config.

---

## 1. Pré-requisitos servidor

- [ ] Paper 1.20+ (ou versão alinhada `api-version`)
- [ ] Plugins: **OBM-Core** primeiro, depois SMP, TierSpace, UHC, Inventory, **Lobby por último**
- [ ] Citizens, PlaceholderAPI, DecentHolograms (hologramas), LuckPerms
- [ ] Multiverse ou mundos já criados: `Lobby`, `world`, `UHC`, `rankedSpawn`

---

## 2. Corrigir / validar NPCs

1. Instalar **Citizens**; criar 3 NPCs no lobby.
2. Nomes sugeridos (display name contém):
   - Rush: `Rush SMP` ou `⚔ Rush`
   - Hardcore: `Hardcore` ou `💀 UHC`
   - TierSpace: `TierSpace` ou `🏆 Ranked`
3. Em `OBM-Core/config.yml`:
   ```yaml
   npc:
     debug: true   # temporário — ver consola [OBM-NPC]
   ```
4. Reiniciar servidor; clicar cada NPC; confirmar TP e mensagem.
5. **Evitar** nomes ambíguos (`SMP Hardcore` → vai para Hardcore).

**Fallback:** `/menu` → slots PvP / Hardcore / SMP (mesma lógica que menu, não fila).

---

## 3. Integrar API key

### Render (`obm-network-backend`)

1. Variável ambiente: `PLUGIN_API_KEY=<segredo-longo>`
2. Deploy / restart serviço Node.

### Servidor Minecraft (`plugins/OBM-Core/config.yml`)

```yaml
backend:
  enabled: true
  base-url: https://SEU-BACKEND.onrender.com
  plugin-key: <MESMO_VALOR_QUE_PLUGIN_API_KEY>
  player-storage:
    enabled: true
```

3. `mvn package` OBM-Core; copiar JAR; `/reload confirm` ou restart.
4. Testar: join jogador → consola sem `403`; log `economy-debug` se activo.

### Verificação rápida (curl)

```bash
curl -H "X-Plugin-Key: SEU_KEY" https://SEU-BACKEND.onrender.com/health
```

---

## 4. Validar modos

| Modo | Teste | Config relevante |
|------|-------|------------------|
| Rush | NPC ou menu slot SMP → mundo `world` | `smp-worlds` |
| Hardcore | NPC/menu com 200k+ coins e lvl 10 | `hardcore.unlock`, `uhc-worlds`, `lives_uhc` |
| TierSpace | NPC tier ou menu PvP → `rankedSpawn` | `ranked-worlds` |
| Fila ranked | `/queue` com permissão | OBM-TierSpace `config.yml` arenas |

---

## 5. Sincronizar dados

1. Confirmar `backend.player-storage.enabled: true`.
2. Jogador online 2+ minutos → verificar PostgreSQL `players.coins` incrementa após ganhos.
3. Após compra na loja → aguardar até 60s → `coins` na DB deve igualar cache (`stats/sync`).
4. Leaderboards lobby: `remote-leaderboard.enabled: true` + URLs tops no config.
5. **Não** editar `data.yml` com jogadores online se backend activo.

---

## 6. LuckPerms (permissões jogadores)

Grupo `default` recomendado (exemplo):

```
obm.lobby, obm.menu
obm.smp.enter, obm.smp.shop, obm.smp.sell, obm.smp.money, obm.smp.market, obm.smp.auction, obm.smp.rank, obm.smp.top
obm.tierspace.use, obm.tierspace.queue
obm.daily, obm.shop.global
```

Staff: prefixos `obm.admin.*`, `obm.stats`, `uhc.revive`.

---

## 7. Testar economia

1. Join → `/money` (com permissão) ou PAPI `%obm_coins%`.
2. Kill em Rush ou `/sell` → saldo sobe na RAM.
3. `/shop` compra → saldo desce na RAM.
4. Quit → rejoin → saldo persistido (API).
5. Admin: painel grant coins → verificar audit + API.

Ver checklist detalhado em [testes.md](testes.md).

---

## 8. Hologramas e tops

1. DecentHolograms instalado.
2. `HologramManager` / Lobby `TopHologramManager` com coordenadas no config lobby.
3. API `/top/coins` (etc.) responde 200 com key.
4. Aguardar 60s após primeiro join para refresh.

---

## 9. Admin bridge (opcional)

Se usar `minespace-admin`:

- Configurar `admin-bridge` no Core (URL + `X-Bridge-Key`) — **separado** do Render.
- Não confundir com `backend.plugin-key`.

---

## 10. Rollback / debug

| Sintoma | Acção |
|---------|-------|
| 403 em tudo | Verificar plugin-key |
| 0 coins sempre | `player-storage.enabled`, fallback datastore |
| NPC silencioso | `npc.debug`, renomear NPC |
| Comandos SMP negados | LuckPerms `obm.smp.*` |
| DB saldo alto vs jogo | Forçar quit; verificar `stats/sync`; planear subtract API |

---

## Documentação relacionada

- [fluxo-servidor.md](fluxo-servidor.md)
- [problemas.md](problemas.md)
- [../../backend-integration.md](../../backend-integration.md)
- [../../player-cache.md](../../player-cache.md)
