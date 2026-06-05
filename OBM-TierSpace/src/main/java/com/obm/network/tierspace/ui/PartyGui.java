package com.obm.network.tierspace.ui;

import com.obm.network.tierspace.party.Party;
import com.obm.network.tierspace.party.PartyService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class PartyGui {

    public static final String TITLE = TierMenuColors.bold(TierMenuColors.accent("PARTY"));

    public static final int SLOT_CREATE = 11;
    public static final int SLOT_LEAVE = 15;
    public static final int SLOT_INVITE_START = 19;
    public static final int SLOT_BACK = 49;

    private final PartyService partyService;

    public PartyGui(PartyService partyService) {
        this.partyService = partyService;
    }

    public Inventory create(Player viewer) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        Optional<Party> party = partyService.getParty(viewer.getUniqueId());

        if (party.isEmpty()) {
            inv.setItem(SLOT_CREATE, named(Material.LIME_DYE, "§a§lCriar Party",
                    TierMenuColors.separator(), "§7Até §f10 §7jogadores", "", TierMenuColors.primary("▶ Clique")));
            inv.setItem(SLOT_LEAVE, named(Material.GRAY_DYE, "§8Sem party", "§7Cria uma party primeiro"));
        } else {
            Party p = party.get();
            boolean leader = p.isLeader(viewer.getUniqueId());
            inv.setItem(SLOT_CREATE, named(Material.PLAYER_HEAD, "§d§lA tua Party",
                    TierMenuColors.separator(),
                    "§7Membros: §f" + p.size() + "/" + PartyService.MAX_SIZE,
                    "§7Líder: §f" + name(p.getLeaderId()),
                    leader ? "§eLíder" : "§7Membro"));
            inv.setItem(SLOT_LEAVE, named(Material.BARRIER, "§cSair da Party",
                    TierMenuColors.separator(), TierMenuColors.primary("▶ Clique")));

            if (leader) {
                int slot = SLOT_INVITE_START;
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (slot > 43 || online.getUniqueId().equals(viewer.getUniqueId())) {
                        continue;
                    }
                    if (partyService.inParty(online.getUniqueId())) {
                        continue;
                    }
                    inv.setItem(slot++, inviteHead(online));
                }
            }
        }

        inv.setItem(SLOT_BACK, named(Material.ARROW, "§7Fechar", "§7Fecha o menu"));
        return inv;
    }

    private ItemStack inviteHead(Player target) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(target);
            meta.setDisplayName("§f" + target.getName());
            meta.setLore(List.of(TierMenuColors.separator(), TierMenuColors.primary("▶ Convidar")));
            head.setItemMeta(meta);
        }
        return head;
    }

    private static ItemStack named(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(List.of(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static String name(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        return player != null ? player.getName() : "?";
    }
}
