package com.obm.network.smp.duel;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

final class DuelItemLedger {

    private final List<ItemStack> brokenItems = new ArrayList<>();
    private final List<ItemStack> securedDrops = new ArrayList<>();

    void trackBroken(ItemStack broken) {
        if (broken == null || broken.getType().isAir()) {
            return;
        }
        brokenItems.add(broken.clone());
    }

    void trackSecuredDrop(ItemStack dropped) {
        if (dropped == null || dropped.getType().isAir()) {
            return;
        }
        securedDrops.add(dropped.clone());
    }

    boolean isEmpty() {
        return brokenItems.isEmpty() && securedDrops.isEmpty();
    }

    List<ItemStack> drainBroken() {
        List<ItemStack> copy = cloneList(brokenItems);
        brokenItems.clear();
        return copy;
    }

    List<ItemStack> drainDrops() {
        List<ItemStack> copy = cloneList(securedDrops);
        securedDrops.clear();
        return copy;
    }

    List<ItemStack> drainAll() {
        List<ItemStack> all = new ArrayList<>();
        all.addAll(drainBroken());
        all.addAll(drainDrops());
        return all;
    }

    void restoreFrom(List<ItemStack> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (ItemStack item : items) {
            if (item == null || item.getType().isAir()) {
                continue;
            }
            securedDrops.add(item.clone());
        }
    }

    private static List<ItemStack> cloneList(List<ItemStack> source) {
        List<ItemStack> copy = new ArrayList<>(source.size());
        for (ItemStack item : source) {
            if (item != null && !item.getType().isAir()) {
                copy.add(item.clone());
            }
        }
        return copy;
    }
}
