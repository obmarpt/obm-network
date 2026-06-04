# MineSpace — Sistema de permissões (produção)

Documentação do sistema final. Comandos executáveis: [`config/luckperms/production-setup.txt`](../config/luckperms/production-setup.txt).

---

## Hierarquia

```
default → vip → mvp → helper → mod → admin → owner
default → builder (ramo paralelo, sem moderação)
```

| Grupo | Peso | Papel |
|-------|------|--------|
| **default** | 0 | Jogador: modos, economia SMP básica, Emeralds, TierSpace fila |
| **vip** | 10 | QoL: chat color, loja VIP SMP, 2º home |
| **mvp** | 20 | QoL+: prefix MVP, workbench, mais homes |
| **builder** | 15 | Construção: WE/Voxel/Citizens, **sem** staff |
| **helper** | 30 | Suporte: kick/mute/vanish/tp |
| **mod** | 40 | Moderação: ban, invsee, fly staff, alertas Grim |
| **admin** | 50 | Seasons, painel OBM, revive UHC, WE extra, bypass AC |
| **owner** | 100 | Gestão LP/TAB/PAPI |

---

## 1. Permissões por grupo

### DEFAULT

**OBM:** `obm.shop.global`, `obm.daily`, `obm.cosmetics`, `obm.crate`, `obm.battlepass`, `obm.season.info`, `obm.lobby`, `obm.menu`, `obm.smp.enter`, `obm.smp.shop`, `obm.smp.sell`, `obm.smp.auction`, `obm.smp.rank`, `obm.smp.money`, `obm.smp.money.pay`, `obm.smp.market`, `obm.smp.top`, `obm.tierspace.use`, `obm.tierspace.queue`, `obm.uhc.stats`

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

**+** `obm.rank.helper`, `tab.staff`, kick/mute/tempmute/unmute, tp/tphere/tpoffline, vanish + see, socialspy, nick, chat.color Essentials

---

### MOD

**+** `obm.rank.mod`, ban/tempban/unban/banip, invsee, enderchest.others, fly, speed, heal, feed, `grim.alerts`

**Sem:** `grim.exempt`, `spartan.bypass`, `obm.admin.panel`, `luckperms.*`

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
# Consola — colar blocos de config/luckperms/production-setup.txt
lp user <nick> parent set default
lp user <nick> parent add vip
```

Recompilar e deploy OBM-* após atualização de permissões no código.
