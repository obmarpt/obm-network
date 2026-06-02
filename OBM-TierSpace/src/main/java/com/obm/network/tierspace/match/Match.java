package com.obm.network.tierspace.match;

import com.obm.network.tierspace.mode.GameModeId;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class Match {

    public enum State {
        COUNTDOWN,
        FIGHTING,
        FINISHED
    }

    public enum EndReason {
        DEATH,
        DISCONNECT,
        TIMEOUT
    }

    private final String id;
    private final GameModeId mode;
    private final String arenaId;
    private final UUID playerOne;
    private final UUID playerTwo;
    private final Location returnOne;
    private final Location returnTwo;
    private final ItemStack[] backupOne;
    private final ItemStack[] backupTwo;
    private final ItemStack[] armorOne;
    private final ItemStack[] armorTwo;
    private final ItemStack offhandOne;
    private final ItemStack offhandTwo;

    private State state = State.COUNTDOWN;

    public Match(String id, GameModeId mode, String arenaId,
                 UUID playerOne, UUID playerTwo,
                 Location returnOne, Location returnTwo,
                 ItemStack[] backupOne, ItemStack[] backupTwo,
                 ItemStack[] armorOne, ItemStack[] armorTwo,
                 ItemStack offhandOne, ItemStack offhandTwo) {
        this.id = id;
        this.mode = mode;
        this.arenaId = arenaId;
        this.playerOne = playerOne;
        this.playerTwo = playerTwo;
        this.returnOne = returnOne;
        this.returnTwo = returnTwo;
        this.backupOne = cloneContents(backupOne);
        this.backupTwo = cloneContents(backupTwo);
        this.armorOne = cloneContents(armorOne);
        this.armorTwo = cloneContents(armorTwo);
        this.offhandOne = offhandOne == null ? null : offhandOne.clone();
        this.offhandTwo = offhandTwo == null ? null : offhandTwo.clone();
    }

    public String id() {
        return id;
    }

    public GameModeId mode() {
        return mode;
    }

    public String arenaId() {
        return arenaId;
    }

    public UUID playerOne() {
        return playerOne;
    }

    public UUID playerTwo() {
        return playerTwo;
    }

    public Location returnOne() {
        return returnOne;
    }

    public Location returnTwo() {
        return returnTwo;
    }

    public ItemStack[] backupOne() {
        return backupOne;
    }

    public ItemStack[] backupTwo() {
        return backupTwo;
    }

    public ItemStack[] armorOne() {
        return armorOne;
    }

    public ItemStack[] armorTwo() {
        return armorTwo;
    }

    public ItemStack offhandOne() {
        return offhandOne;
    }

    public ItemStack offhandTwo() {
        return offhandTwo;
    }

    public State state() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public boolean involves(UUID uuid) {
        return playerOne.equals(uuid) || playerTwo.equals(uuid);
    }

    public UUID opponent(UUID uuid) {
        if (playerOne.equals(uuid)) {
            return playerTwo;
        }
        if (playerTwo.equals(uuid)) {
            return playerOne;
        }
        return null;
    }

    private ItemStack[] cloneContents(ItemStack[] source) {
        if (source == null) {
            return new ItemStack[36];
        }
        ItemStack[] copy = new ItemStack[source.length];
        for (int i = 0; i < source.length; i++) {
            copy[i] = source[i] == null ? null : source[i].clone();
        }
        return copy;
    }
}
