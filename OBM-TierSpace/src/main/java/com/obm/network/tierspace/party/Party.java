package com.obm.network.tierspace.party;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public final class Party {

    private final UUID leaderId;
    private final Set<UUID> members = new LinkedHashSet<>();
    private final Set<UUID> invites = new LinkedHashSet<>();

    public Party(UUID leaderId) {
        this.leaderId = leaderId;
        this.members.add(leaderId);
    }

    public UUID getLeaderId() {
        return leaderId;
    }

    public Set<UUID> getMembers() {
        return Set.copyOf(members);
    }

    public int size() {
        return members.size();
    }

    public boolean isMember(UUID uuid) {
        return members.contains(uuid);
    }

    public boolean isLeader(UUID uuid) {
        return leaderId.equals(uuid);
    }

    public boolean addMember(UUID uuid) {
        invites.remove(uuid);
        return members.add(uuid);
    }

    public boolean removeMember(UUID uuid) {
        if (leaderId.equals(uuid)) {
            return false;
        }
        return members.remove(uuid);
    }

    public void invite(UUID uuid) {
        if (!members.contains(uuid)) {
            invites.add(uuid);
        }
    }

    public boolean hasInvite(UUID uuid) {
        return invites.contains(uuid);
    }

    public void clearInvite(UUID uuid) {
        invites.remove(uuid);
    }
}
