package com.obm.network.smp.service;

import com.obm.network.core.OBMCorePlugin;
import com.obm.network.core.integration.EconomyBridge;
import com.obm.network.core.storage.DataStore;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EconomyService {

    private static final String TOTAL_EARNED_KEY = "smp_total_earned";
    private static final String TOTAL_SPENT_KEY = "smp_total_spent";

    private static final int MAX_BALANCE = Integer.MAX_VALUE / 2;

    private final Economy economy;
    private final int startingBalance;
    private final DataStore dataStore;
    private final ConcurrentHashMap<UUID, Object> balanceLocks = new ConcurrentHashMap<>();

    public EconomyService(Economy economy, int startingBalance) {
        this.economy = economy;
        this.startingBalance = Math.max(0, startingBalance);
        this.dataStore = OBMCorePlugin.get().getDataStore();
    }

    public boolean isEnabled() {
        return economy != null;
    }

    private OfflinePlayer getOfflinePlayer(UUID uuid) {
        return Bukkit.getOfflinePlayer(uuid);
    }

    private void ensureVaultAccount(UUID uuid) {
        if (!isEnabled()) {
            return;
        }
        OfflinePlayer player = getOfflinePlayer(uuid);
        if (!economy.hasAccount(player)) {
            economy.createPlayerAccount(player);
        }
    }

    /**
     * Garante chave {@link EconomyBridge#BALANCE_KEY} no DataStore (migra Vault legado se existir).
     */
    private void ensureBalanceInitialized(UUID uuid) {
        if (dataStore.has(uuid, EconomyBridge.BALANCE_KEY)) {
            return;
        }

        ensureVaultAccount(uuid);
        int vaultBalance = readVaultBalance(uuid);
        int initial = vaultBalance > 0 ? vaultBalance : startingBalance;
        dataStore.set(uuid, EconomyBridge.BALANCE_KEY, initial);
        syncVaultToMatch(uuid, initial);
    }

    private int readVaultBalance(UUID uuid) {
        if (!isEnabled()) {
            return 0;
        }
        ensureVaultAccount(uuid);
        return Math.max(0, (int) Math.floor(economy.getBalance(getOfflinePlayer(uuid))));
    }

    /** Alinha Vault com o saldo interno (best-effort). */
    private void syncVaultToMatch(UUID uuid, int targetBalance) {
        if (!isEnabled()) {
            return;
        }
        ensureVaultAccount(uuid);
        OfflinePlayer player = getOfflinePlayer(uuid);
        int vault = Math.max(0, (int) Math.floor(economy.getBalance(player)));
        if (vault < targetBalance) {
            economy.depositPlayer(player, targetBalance - vault);
        } else if (vault > targetBalance) {
            economy.withdrawPlayer(player, vault - targetBalance);
        }
    }

    public int getBalance(UUID uuid) {
        ensureBalanceInitialized(uuid);
        return Math.max(0, dataStore.getInt(uuid, EconomyBridge.BALANCE_KEY));
    }

    public boolean canAfford(UUID uuid, int amount) {
        return amount <= 0 || getBalance(uuid) >= amount;
    }

    public EconomyResponse deposit(UUID uuid, int amount) {
        if (amount <= 0) {
            return new EconomyResponse(0, getBalance(uuid), EconomyResponse.ResponseType.SUCCESS, "No deposit needed");
        }

        synchronized (lockFor(uuid)) {
            ensureBalanceInitialized(uuid);
            long newBalanceLong = (long) getBalance(uuid) + amount;
            int newBalance = (int) Math.min(MAX_BALANCE, newBalanceLong);
            dataStore.set(uuid, EconomyBridge.BALANCE_KEY, newBalance);
            trackEarned(uuid, amount);
            syncVaultToMatch(uuid, newBalance);
            dataStore.save(uuid);
            notifyRemoteStorage(uuid, newBalance);
            org.bukkit.entity.Player online = Bukkit.getPlayer(uuid);
            if (online != null) {
                com.obm.network.core.integration.BattlePassBridge.smpMoneyEarned(online, amount);
                com.obm.network.core.integration.AchievementBridge.smpMoney(online);
                com.obm.network.core.ui.PlayerUx.notifyMoneyGain(online, amount, format(amount));
            } else {
                com.obm.network.core.integration.BattlePassBridge.smpMoneyEarned(uuid, amount);
                com.obm.network.core.integration.AchievementBridge.smpMoney(uuid);
            }
            return new EconomyResponse(amount, newBalance, EconomyResponse.ResponseType.SUCCESS, null);
        }
    }

    public EconomyResponse withdraw(UUID uuid, int amount) {
        if (amount <= 0) {
            return new EconomyResponse(0, getBalance(uuid), EconomyResponse.ResponseType.SUCCESS, "No withdraw needed");
        }

        synchronized (lockFor(uuid)) {
            ensureBalanceInitialized(uuid);
            int balance = getBalance(uuid);
            if (balance < amount) {
                return new EconomyResponse(0, balance, EconomyResponse.ResponseType.FAILURE, "Insufficient funds");
            }

            int newBalance = balance - amount;
            dataStore.set(uuid, EconomyBridge.BALANCE_KEY, newBalance);
            trackSpent(uuid, amount);
            syncVaultToMatch(uuid, newBalance);
            dataStore.save(uuid);
            notifyRemoteStorage(uuid, newBalance);
            return new EconomyResponse(amount, newBalance, EconomyResponse.ResponseType.SUCCESS, null);
        }
    }

    private Object lockFor(UUID uuid) {
        return balanceLocks.computeIfAbsent(uuid, ignored -> new Object());
    }

    private void notifyRemoteStorage(UUID uuid, int newBalance) {
        try {
            Class<?> hook = Class.forName("com.obm.network.core.storage.remote.BackendStorageHook");
            hook.getMethod("onSmpCoinsChanged", UUID.class, int.class).invoke(null, uuid, newBalance);
        } catch (ReflectiveOperationException ignored) {
            // OBM-Core antigo ou storage desactivado
        }
    }

    public boolean safeWithdraw(UUID uuid, int amount) {
        if (amount <= 0) {
            return true;
        }
        int balance = getBalance(uuid);
        int toWithdraw = Math.min(balance, amount);
        if (toWithdraw <= 0) {
            return false;
        }
        return withdraw(uuid, toWithdraw).transactionSuccess();
    }

    public boolean transfer(UUID source, UUID target, int amount) {
        if (amount <= 0 || source == null || target == null || source.equals(target)) {
            return false;
        }
        UUID first = source.compareTo(target) <= 0 ? source : target;
        UUID second = first.equals(source) ? target : source;
        Object lock1 = balanceLocks.computeIfAbsent(first, ignored -> new Object());
        Object lock2 = balanceLocks.computeIfAbsent(second, ignored -> new Object());
        synchronized (lock1) {
            synchronized (lock2) {
                if (!canAfford(source, amount)) {
                    return false;
                }
                EconomyResponse withdrawResponse = withdraw(source, amount);
                if (!withdrawResponse.transactionSuccess()) {
                    return false;
                }
                EconomyResponse depositResponse = deposit(target, amount);
                if (!depositResponse.transactionSuccess()) {
                    deposit(source, amount);
                    return false;
                }
                return true;
            }
        }
    }

    public int getTotalEarned(UUID uuid) {
        return dataStore.getInt(uuid, TOTAL_EARNED_KEY);
    }

    public int getTotalSpent(UUID uuid) {
        return dataStore.getInt(uuid, TOTAL_SPENT_KEY);
    }

    private void trackEarned(UUID uuid, int amount) {
        if (amount <= 0) {
            return;
        }
        dataStore.set(uuid, TOTAL_EARNED_KEY, dataStore.getInt(uuid, TOTAL_EARNED_KEY) + amount);
    }

    private void trackSpent(UUID uuid, int amount) {
        if (amount <= 0) {
            return;
        }
        dataStore.set(uuid, TOTAL_SPENT_KEY, dataStore.getInt(uuid, TOTAL_SPENT_KEY) + amount);
    }

    public String format(int amount) {
        return com.obm.network.core.economy.CurrencyLabels.formatSmpMoney(amount);
    }
}
