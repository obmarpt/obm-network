# Roadmap — prioridades

Ordem sugerida: **estabilidade → economia/backend → gameplay → polish**.

---

## Fase 1 — Produção estável (P0)

| Item | Estado | Acção |
|------|--------|-------|
| API key Render | Parcial | Deploy com `PLUGIN_API_KEY`; espelhar em `config.yml` |
| NPCs lobby | Corrigido no código | Validar nomes Citizens em produção |
| `/lobby` / `/menu` | Corrigido | Testar após reload LuckPerms |
| Cache + sync 30s | Implementado | Monitorar logs `economy-debug` |
| `stats/sync` absoluto | Implementado | Confirmar kills/playtime na DB |

---

## Fase 2 — Economia coerente (P1)

| Item | Prioridade | Descrição |
|------|------------|-----------|
| `POST /money/subtract` ou delta negativo | Alta | Evitar inflação temporária na DB |
| Emeralds na API | Média | Unificar moedas network no PostgreSQL |
| Withdraw admin → backend | Média | `AdminEconomyService` alinhar com API |
| Dashboard coins live | Média | UI consumir `GET /player/:uuid` ou WebSocket |
| Unificar `/top` com API | Baixa | Um só leaderboard Rush |

---

## Fase 3 — Gameplay e modos (P2)

| Item | Descrição |
|------|-----------|
| Hardcore seasons | `SeasonService` hardcore info only — avaliar reset vidas |
| TierSpace matchmaking | Arenas, kits, placement — testar fila pico |
| Rush SMP retenção | Milestones, battle pass ligado a coins |
| Menu TierSpace dedicado | Slot ou submenu para `/queue` no lobby |
| PvP slot → fila | Clarificar UX: hub vs queue |

---

## Fase 4 — Staff e observabilidade (P2)

| Item | Descrição |
|------|-----------|
| minespace-admin + Render | Documentar qual painel mostra o quê |
| Alertas Discord TierSpace | `DiscordWebhookService` configurar URL |
| Audit unificado | `/log` Render + bridge admin |
| Métricas Render cold start | Timeouts join; aumentar `read-timeout-ms` se necessário |

---

## Fase 5 — Polish (P3)

| Item | Descrição |
|------|-----------|
| Cosmetics / crates / BP | Conteúdo YAML + balance Emeralds |
| Global shop search | UX e preços |
| ALT detection tuning | IPs partilhados legítimos (LAN cafés) |
| Multiverse templates | Mundos UHC/ranked isolados |
| Testes automatizados | Mock HTTP ou integration test harness |

---

## Sistemas incompletos (inventário)

- **Dashboard web** economia jogador em tempo real (não wired ao Render no repo admin).
- **Inventário/posição** na API (só YAML + OBM-Inventory).
- **Rank network vs SMP rank** — dois sistemas (`RankService` Core vs `RankService` SMP).
- **UHC revive** — staff only; sem integração backend.
- **Remote commands** — existe mas requer API segura e UI admin.

---

## Critérios de “servidor completo”

- [ ] Join carrega saldo API em &lt; 12s
- [ ] NPC Rush/Hardcore/Tier funcionam 10/10 cliques
- [ ] Comprar na loja SMP reduz saldo e DB coincide após 60s
- [ ] Hologramas tops atualizam após ganhos
- [ ] Hardcore bloqueia sem unlock; permite com unlock + vidas
- [ ] `/queue` TierSpace completa match em arena
- [ ] Staff admin panel + logs visíveis no painel
