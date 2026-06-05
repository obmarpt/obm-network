# Documentação — Plugins MineSpace (OBM)

Análise do código-fonte em `D:\PluginsMinecraft\OMD Network` (junho 2026). **Nenhuma alteração de código** — apenas documentação funcional baseada no estado actual do repositório.

## Índice

| # | Documento | Conteúdo |
|---|-----------|----------|
| 1 | [estrutura.md](estrutura.md) | Plugins, dependências, interacções |
| 2 | [modos.md](modos.md) | Rush SMP, Hardcore, TierSpace |
| 3 | [comandos.md](comandos.md) | Todos os comandos OBM |
| 4 | [eventos.md](eventos.md) | Join, economia, NPCs, admin |
| 5 | [classes.md](classes.md) | Classes por módulo (índice) |
| 6 | [fluxo-servidor.md](fluxo-servidor.md) | Fluxo lobby → backend → dashboard |
| 7 | [problemas.md](problemas.md) | Bugs e riscos conhecidos |
| 8 | [roadmap.md](roadmap.md) | O que falta / prioridades |
| 9 | [passos-implementacao.md](passos-implementacao.md) | Guia operacional |
| 10 | [testes.md](testes.md) | Checklist QA |

## Docs relacionados (raiz `docs/`)

- [server-overview.md](../../server-overview.md) — visão geral permanente
- [backend-integration.md](../../backend-integration.md) — API Render
- [player-cache.md](../../player-cache.md) — cache RAM + HTTP
- [leaderboards.md](../../leaderboards.md) — tops + hologramas
- [minespace-admin-integration.md](../../minespace-admin-integration.md) — painel web (bridge separada)

## Backend e painéis externos

| Sistema | Pasta | Auth |
|---------|-------|------|
| API jogadores/economia | `obm-network-backend/` | `X-Plugin-Key` |
| Admin web + Discord | `minespace-admin/` | `X-Bridge-Key` |
