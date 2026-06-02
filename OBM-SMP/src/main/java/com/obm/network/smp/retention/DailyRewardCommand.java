package com.obm.network.smp.retention;

import com.obm.network.core.OBMCorePlugin;
import com.obm.network.core.storage.DataStore;
import com.obm.network.smp.SMPPlugin;
import com.obm.network.smp.service.EconomyService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.concurrent.TimeUnit;

public class DailyRewardCommand implements CommandExecutor {

    private static final String LAST_CLAIM_KEY = "smp_daily_last_claim";

    private final SMPPlugin plugin;
    private final EconomyService economy;

    public DailyRewardCommand(SMPPlugin plugin, EconomyService economy) {
        this.plugin = plugin;
        this.economy = economy;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cApenas jogadores.");
            return true;
        }

        if (!plugin.getConfig().getBoolean("rewards.daily.enabled", true)) {
            player.sendMessage("§cDaily rewards estão desativadas.");
            return true;
        }

        int coins = plugin.getConfig().getInt("rewards.daily.coins", 2000);
        long cooldownHours = plugin.getConfig().getLong("rewards.daily.cooldown-hours", 24L);
        long cooldownMs = TimeUnit.HOURS.toMillis(cooldownHours);

        DataStore ds = OBMCorePlugin.get().getDataStore();
        long now = System.currentTimeMillis();
        long last = ds.getLong(player.getUniqueId(), LAST_CLAIM_KEY);

        if (last > 0 && now - last < cooldownMs) {
            long remaining = cooldownMs - (now - last);
            RetentionFeedback.dailyAlreadyClaimed(player, formatDuration(remaining));
            return true;
        }

        economy.deposit(player.getUniqueId(), coins);
        ds.set(player.getUniqueId(), LAST_CLAIM_KEY, now);
        ds.save(player.getUniqueId());

        RetentionFeedback.dailySuccess(player, coins);
        return true;
    }

    private static String formatDuration(long millis) {
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        return minutes + "m";
    }
}
