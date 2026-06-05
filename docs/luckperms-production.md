# MineSpace — Sistema de permissões (produção)

Documentação do sistema final. **Setup completo:** [`config/luckperms/permissions-complete.txt`](../config/luckperms/permissions-complete.txt). Legado/deltas: [`production-setup.txt`](../config/luckperms/production-setup.txt), [`production-delta-v2.txt`](../config/luckperms/production-delta-v2.txt).

---

## Hierarquia

```
Jogador:  default → vip → vip_plus → mvp → mvp_plus
Staff:    default → helper → mod (Moderator) → admin → owner
Paralelo: default → builder (construção, sem moderação)
```

| Grupo | Peso | Papel |
|-------|------|--------|
| **default** | 0 | Jogador: spawn/home/msg/duel, economia SMP, Emeralds, TierSpace |
| **vip** | 10 | QoL: chat color, loja VIP SMP, `/repair`, 2º home |
| **vip_plus** | 15 | Kits VIP+, fly, repair Essentials |
| **mvp** | 20 | Warp MVP, prioridade queue, workbench, mais homes |
| **mvp_plus** | 25 | Repair all, máximo de homes |
| **builder** | 15 | Construção: WE/Voxel/Citizens, **sem** staff |
| **helper** | 30 | Suporte: kick/mute, `/report list` — **sem** ban/tp/vanish |
| **mod** | 40 | Moderator: ban, tp, vanish, invsee, alertas Grim |
| **admin** | 50 | Painel OBM, economia staff, revive UHC, fly staff |
| **owner** | 100 | Tudo + gestão LP/TAB/PAPI + bypass AC |

---

## 1. Permissões por grupo

### DEFAULT

**OBM:** `obm.help`, `obm.report`, `obm.link`, `obm.achievements`, `obm.duel.use`, `obm.rankup.use`, `obm.crate.open`, `obm.shop.global`, `obm.daily`, `obm.cosmetics`, `obm.battlepass`, `obm.season.info`, `obm.lobby`, `obm.menu`, `obm.smp.*` (enter/shop/sell/auction/rank/money/market/top), `obm.tierspace.use`, `obm.tierspace.queue`, `obm.uhc.stats`

**Nota:** `obm.smp.rank` = progressão SMP (comprar ranks in-game). **Não** confundir com `obm.rank.mod` (display staff).

**Essentials:** spawn, help, list, msg, reply, afk, home/sethome/delhome (1), tpa/tpaccept/tpdeny/tpahere, back, ignore, mail

**Comandos:** `/menu`, `/lobby`, `/smp`, `/shop`, `/sell`, `/ah`, `/money`, `/money pay`, `/market`, `/rank`, `/top`, `/daily`, `/cosmetics`, `/crate`, `/battlepass`, `/shopglobal`, `/season`, `/queue`, `/tierspace`, `/uhcstats`

**Limitações:** sem admin, sem stats alheios, sem revive, sem WE, sem fly/god/vanish Essentials, **sem** `essentials.pay`

---

### VIP

**+** `group.vip`, `obm.smp.vip`, `obm.chat.color`, `essentials.hat`, `essentials.sethome.multiple` + `essentials.sethome.multiple.vip`

**Funcional:** categoria **raros** na loja SMP, multiplicador coins VIP (config), cores no chat

**Não inclui:** pay-to-win extra, admin, WE

---

### MVP

**+** `group.mvp`, `essentials.sethome.multiple.mvp`, `essentials.workbench`

**Não inclui:** moderação, bypass anticheat

---

### BUILDER

**+** `obm.rank.builder`, `essentials.build` (+ break/place)

**WorldEdit (exato):** `worldedit.wand`, `worldedit.selection.pos`, `worldedit.selection.hpos`, `worldedit.selection.expand`, `worldedit.selection.contract`, `worldedit.selection.shift`, `worldedit.region.set`, `worldedit.region.replace`, `worldedit.clipboard.copy`, `worldedit.clipboard.paste`, `worldedit.clipboard.rotate`, `worldedit.undo`, `worldedit.redo`

**VoxelSniper:** `voxelsniper.sniper`, `voxelsniper.brush`

**Citizens:** `citizens.npc.create`, `citizens.npc.edit`, `citizens.npc.use`

**Não inclui:** `worldedit.*`, `spartan.bypass`, `grim.exempt`, ban/kick, `obm.admin.*`, `luckperms.*`

---

### HELPER

**+** `obm.rank.helper`, `obm.staff.reports`, `tab.staff`, kick/mute/tempmute/unmute, socialspy, nick, `obm.chat.staff`

**Sem:** ban, tp, vanish, `obm.admin.*`

---

### MOD (Moderator — grupo LP `mod`)

**+** `obm.rank.mod`, ban/tempban/unban/banip, tp/tphere/vanish, invsee, enderchest.others, speed, heal, feed, `grim.alerts`

**Sem:** fly (só admin+), `grim.exempt`, `spartan.bypass`, `obm.admin.panel`, `luckperms.*`

---

### ADMIN

**+** `obm.rank.admin`, `obm.admin.panel`, `obm.admin.season`, `tierspace.admin.season`, `obm.smp.admin`, `obm.stats`, `uhc.revive`, gamemode/time/weather/clear/give, `citizens.admin`, Multiverse access básico, `grim.exempt`, `spartan.bypass`, `luckperms.user.info`, `luckperms.group.info`, WE clipboard clear / region clear / biomes

---

### OWNER

**+** `obm.rank.owner`, `luckperms.user.edit`, `luckperms.group.edit`, `luckperms.track.edit`, `luckperms.reloadconfig`, `tab.reload`, `papi.reload`

**Nota:** ainda **sem** `luckperms.*` nem `*` — permissões LP granulares.

---

## 2. Permissões perigosas (nunca em default/vip/mvp/builder)

| Permissão | Risco |
|-----------|--------|
| `*` / `essentials.*` / `worldedit.*` / `luckperms.*` | Acesso total |
| `obm.admin.panel` | Reset seasons/economia/UHC |
| `obm.smp.admin` | Bypass toda economia SMP |
| `obm.stats` | Ver stats de qualquer jogador |
| `uhc.revive` | Revive em massa |
| `spartan.bypass` / `grim.exempt` | Desactiva anticheat |
| `essentials.give` / `essentials.eco` | Duplicação economia |
| `essentials.pay` | Conflito com `/money pay` OBM |

---

## 3. Conflitos resolvidos

1. **Economia:** coins via OBM-SMP + Vault; Emeralds via OBM-Core. Essentials `pay`/`balance` **negados** no default e desactivados em `Essentials/config.yml`.
2. **TAB:** usar `%obm_coins%` e `%obm_emeralds%` no footer (não só Vault).
3. **Admin panel:** acções de season exigem `obm.admin.season` no código (além de `obm.admin.panel` para abrir GUI).

---

## 4. Recomendações de segurança

- Atribuir **um** grupo principal por jogador (`parent set`) + extras (`parent add builder`).
- Revisar `plugins/WorldEdit/config.yml`: definir `max-blocks-changed` por grupo LP (ex. builder 50k, admin 500k).
- Não dar `spartan.bypass` permanente a MOD; só ADMIN em investigações.
- Exportar LP após mudanças: `lp export file backups/lp-$(date +%Y%m%d).json`
- Testar: jogador default não executa `/lp`, `//set` sem builder, `/adminpanel` sem admin.

---

## 5. Melhorias futuras (opcional)

- `obm.smp.market.listing.limit.10` por grupo VIP
- Limite diário `obm.smp.money.pay.max` no código
- Permissão `obm.tierspace.queue.<mode>` por modo
- LuckPerms YAML storage no repo para versionar grupos

---

## Delta v2 (melhorias segurança)

Ver [`config/luckperms/production-delta-v2.txt`](../config/luckperms/production-delta-v2.txt) se o servidor **já** tinha o setup inicial.

**Mudanças:** MOD sem fly; fly só ADMIN+; bypass AC só OWNER; `obm.chat.staff` em HELPER+; limite WE builder 50k blocos; cooldowns pay/market no código.

---

## 6. Aplicar no servidor

```bash
# Consola — colar config/luckperms/permissions-complete.txt (servidor novo)
lp user <nick> parent set default
lp user <nick> parent add vip
lp user <staff> parent add helper   # track staff paralelo ao VIP
```

Recompilar e deploy OBM-* após atualização de permissões no código.
