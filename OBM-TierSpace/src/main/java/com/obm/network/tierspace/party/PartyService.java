package com.obm.network.tierspace.party;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PartyService {

    public static final int MAX_SIZE = 10;

    private final Map<UUID, Party> partiesByLeader = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> partyByMember = new ConcurrentHashMap<>();

    public Optional<Party> getParty(UUID memberId) {
        UUID leader = partyByMember.get(memberId);
        if (leader == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(partiesByLeader.get(leader));
    }

    public boolean inParty(UUID uuid) {
        return partyByMember.containsKey(uuid);
    }

    public Party create(Player leader) {
        UUID leaderId = leader.getUniqueId();
        leave(leaderId);
        Party party = new Party(leaderId);
        partiesByLeader.put(leaderId, party);
        partyByMember.put(leaderId, leaderId);
        return party;
    }

    public boolean invite(Player leader, Player target) {
        Optional<Party> partyOpt = getParty(leader.getUniqueId());
        if (partyOpt.isEmpty() || !partyOpt.get().isLeader(leader.getUniqueId())) {
            return false;
        }
        Party party = partyOpt.get();
        if (party.size() >= MAX_SIZE) {
            leader.sendMessage("§cParty cheia (máx. " + MAX_SIZE + ").");
            return false;
        }
        if (inParty(target.getUniqueId())) {
            leader.sendMessage("§cEsse jogador já tem party.");
            return false;
        }
        party.invite(target.getUniqueId());
        leader.sendMessage("§dParty §8| §7Convite enviado a §f" + target.getName());
        target.sendMessage("§dParty §8| §f" + leader.getName() + " §7convidou-te. §a/party accept");
        return true;
    }

    public boolean accept(Player player) {
        for (Party party : partiesByLeader.values()) {
            if (!party.hasInvite(player.getUniqueId())) {
                continue;
            }
            if (party.size() >= MAX_SIZE) {
                player.sendMessage("§cA party já está cheia.");
                party.clearInvite(player.getUniqueId());
                return false;
            }
            leave(player.getUniqueId());
            party.addMember(player.getUniqueId());
            partyByMember.put(player.getUniqueId(), party.getLeaderId());
            broadcast(party, "§a" + player.getName() + " entrou na party.");
            return true;
        }
        player.sendMessage("§cSem convites pendentes.");
        return false;
    }

    public void leave(UUID uuid) {
        UUID leader = partyByMember.remove(uuid);
        if (leader == null) {
            return;
        }
        Party party = partiesByLeader.get(leader);
        if (party == null) {
            return;
        }
        if (party.isLeader(uuid)) {
            disband(leader);
            return;
        }
        party.removeMember(uuid);
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            player.sendMessage("§7Saíste da party.");
        }
        broadcast(party, "§7" + name(uuid) + " saiu da party.");
    }

    public boolean kick(Player leader, UUID target) {
        Optional<Party> partyOpt = getParty(leader.getUniqueId());
        if (partyOpt.isEmpty() || !partyOpt.get().isLeader(leader.getUniqueId())) {
            return false;
        }
        Party party = partyOpt.get();
        if (!party.removeMember(target)) {
            return false;
        }
        partyByMember.remove(target);
        Player targetPlayer = Bukkit.getPlayer(target);
        if (targetPlayer != null) {
            targetPlayer.sendMessage("§cFoste removido da party.");
        }
        broadcast(party, "§c" + name(target) + " foi removido.");
        return true;
    }

    private void disband(UUID leaderId) {
        Party party = partiesByLeader.remove(leaderId);
        if (party == null) {
            return;
        }
        for (UUID member : party.getMembers()) {
            partyByMember.remove(member);
            Player online = Bukkit.getPlayer(member);
            if (online != null && !member.equals(leaderId)) {
                online.sendMessage("§7A party foi dissolvida.");
            }
        }
    }

    private void broadcast(Party party, String message) {
        for (UUID member : party.getMembers()) {
            Player online = Bukkit.getPlayer(member);
            if (online != null) {
                online.sendMessage("§dParty §8| " + message);
            }
        }
    }

    private static String name(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        return player != null ? player.getName() : uuid.toString().substring(0, 8);
    }
}
