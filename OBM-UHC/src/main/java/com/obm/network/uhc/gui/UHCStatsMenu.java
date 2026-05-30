package com.obm.network.uhc.gui;

import com.obm.network.core.OBMCorePlugin;
import com.obm.network.core.storage.DataStore;
import com.obm.network.uhc.util.UHCUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.UUID;

public class UHCStatsMenu {

    private static final DataStore ds = OBMCorePlugin.get().getDataStore();
    
    private static final String KILLS_KEY = "kills_uhc";
    private static final String DEATHS_KEY = "deaths_uhc";
    private static final String TIME_ALIVE_KEY = "time_alive_uhc";
    private static final String MOBS_KEY = "mobs_uhc";

    public static void open(Player player) {
        UUID uuid = player.getUniqueId();

        // 1. Criação do Inventário
        Inventory inv = Bukkit.createInventory(null, 27, 
            Component.text("UHC STATS", NamedTextColor.RED).decorate(TextDecoration.BOLD)
        );

        // 2. Coleta de dados
        int lives = UHCUtils.getLives(player);
        int kills = ds.getInt(uuid, KILLS_KEY);
        int deaths = ds.getInt(uuid, DEATHS_KEY);
        int mobs = ds.getInt(uuid, MOBS_KEY);
        
        int totalSeconds = ds.getInt(uuid, TIME_ALIVE_KEY);
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        String timeFormatted = hours + "h " + minutes + "m";

        Component protectionComponent;
        if (UHCUtils.hasProtection(player)) {
            long remainingMs = UHCUtils.getRemainingProtection(player);
            long totalRemainingSec = remainingMs / 1000;
            long protectionHours = totalRemainingSec / 3600;
            long protectionMinutes = (totalRemainingSec % 3600) / 60;

            protectionComponent = Component.text(protectionHours + "h " + protectionMinutes + "m", NamedTextColor.GREEN);
        } else {
            protectionComponent = Component.text("Hardcore ativo", NamedTextColor.RED);
        }

        // 3. Item de Estatísticas
        ItemStack statsItem = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta statsMeta = statsItem.getItemMeta();

        if (statsMeta != null) {
            statsMeta.displayName(Component.text("As tuas estatísticas", NamedTextColor.RED).decoration(TextDecoration.BOLD, false));
            statsMeta.lore(List.of(
                Component.empty(),
                Component.text("❤ Vidas: ", NamedTextColor.GRAY).append(Component.text(lives, NamedTextColor.RED)),
                Component.text("⚔ Kills: ", NamedTextColor.GRAY).append(Component.text(kills, NamedTextColor.GREEN)),
                Component.text("☠ Mobs: ", NamedTextColor.GRAY).append(Component.text(mobs, NamedTextColor.WHITE)),
                Component.text("☠ Mortes: ", NamedTextColor.GRAY).append(Component.text(deaths, NamedTextColor.DARK_RED)),
                Component.text("⏳ Tempo Vivo: ", NamedTextColor.GRAY).append(Component.text(timeFormatted, NamedTextColor.GOLD)),
                Component.text("🛡️ Proteção: ", NamedTextColor.GRAY).append(protectionComponent)
            ));
            statsItem.setItemMeta(statsMeta);
        }
        inv.setItem(13, statsItem);

        // 4. Vidros de Decoração
        ItemStack glassPane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glassPane.getItemMeta();
        if (glassMeta != null) {
            glassMeta.displayName(Component.empty());
            glassPane.setItemMeta(glassMeta);
        }

        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, glassPane);
            }
        }

        player.openInventory(inv);
    }
}