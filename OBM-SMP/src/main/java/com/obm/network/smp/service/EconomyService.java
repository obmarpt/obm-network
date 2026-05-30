package com.obm.network.smp.service;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

public class EconomyService {

    private final Economy economy;
    private final int startingBalance;

    public EconomyService(Economy economy, int startingBalance) {
        this.economy = economy;
        this.startingBalance = Math.max(0, startingBalance);
    }

    public boolean isEnabled() {
        return economy != null;
    }

    private OfflinePlayer getOfflinePlayer(UUID uuid) {
        return Bukkit.getOfflinePlayer(uuid);
    }

    private void ensureAccount(UUID uuid) {
        OfflinePlayer player = getOfflinePlayer(uuid);
        if (!economy.hasAccount(player)) {
            economy.createPlayerAccount(player);
        }
    }

    public int getBalance(UUID uuid) {
        ensureAccount(uuid);
        return (int) Math.floor(economy.getBalance(getOfflinePlayer(uuid)));
    }

    public boolean canAfford(UUID uuid, int amount) {
        return getBalance(uuid) >= amount;
    }

    public EconomyResponse deposit(UUID uuid, int amount) {
        if (amount <= 0) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.SUCCESS, "No deposit needed");
        }

        ensureAccount(uuid);
        return economy.depositPlayer(getOfflinePlayer(uuid), amount);
    }

    public EconomyResponse withdraw(UUID uuid, int amount) {
        if (amount <= 0) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.SUCCESS, "No withdraw needed");
        }

        ensureAccount(uuid);
        return economy.withdrawPlayer(getOfflinePlayer(uuid), amount);
    }

    public boolean transfer(UUID source, UUID target, int amount) {
        if (amount <= 0 || !canAfford(source, amount)) {
            return false;
        }

        ensureAccount(target);
        EconomyResponse withdrawResponse = withdraw(source, amount);
        if (!withdrawResponse.transactionSuccess()) {
            return false;
        }

        EconomyResponse depositResponse = deposit(target, amount);
        return depositResponse.transactionSuccess();
    }

    public String format(int amount) {
        return String.valueOf(amount) + " coins";
    }
}
