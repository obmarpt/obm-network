package com.obm.network.tierspace.feedback;



import com.obm.network.core.tier.TierRankUtil;

import com.obm.network.tierspace.rating.RatingChange;

import net.md_5.bungee.api.ChatMessageType;

import net.md_5.bungee.api.chat.TextComponent;

import org.bukkit.Particle;

import org.bukkit.Sound;

import org.bukkit.entity.Player;



public class MatchFeedbackService {



    public void sendWin(Player player, RatingChange change, String opponentName, boolean inPlacement) {

        String subtitle = buildWinSubtitle(change);

        player.sendTitle("§a§lVITÓRIA", subtitle, 5, 55, 12);

        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.3f);



        sendActionBar(player, buildWinActionBar(change, inPlacement));

        player.sendMessage("§a✔ §7Venceste §f" + opponentName + " §7→ §a+" + change.delta() + " rating");



        if (inPlacement) {

            player.sendMessage("§7Placement em progresso — rank oculto até completares 10 jogos.");

        } else {

            sendProgressLine(player, change);

            announceRankShift(player, change);

        }



        if (change.dailyBonus() > 0) {

            sendDailyBonus(player, change.dailyBonus());

        }



        if (change.promoted()) {

            playWinCelebration(player);

        } else {

            player.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, player.getLocation().add(0, 1.2, 0),

                    8, 0.4, 0.3, 0.4, 0);

        }

    }



    public void sendLoss(Player player, RatingChange change, String opponentName,

                         boolean inPlacement, int demotionShieldMax) {

        player.sendTitle("§c§lDERROTA", "§7" + change.delta() + " rating", 5, 55, 12);

        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 0.9f);



        if (change.hadStreakOnLoss()) {

            player.sendTitle("§4§lSTREAK PERDIDA", "§7Estavas com §e" + change.lostStreak(), 5, 40, 10);

            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.6f, 0.7f);

        }



        if (change.demotionShielded()) {

            player.sendMessage("§e§lESCUDO DE DIVISÃO §7→ Protecção activa §7("

                    + change.newRatingLossStreak() + "/" + demotionShieldMax + ")");

            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.6f, 1.2f);

        }



        String rankLabel = inPlacement ? "§ePlacement" : change.newRank().displayName();

        sendActionBar(player, "§c" + change.delta() + " rating §8| §f" + rankLabel);

        player.sendMessage("§c✘ §7Perdeste contra §f" + opponentName + " §7→ §c" + change.delta() + " rating");



        if (!inPlacement) {

            sendProgressLine(player, change);

            if (!change.demotionShielded()) {

                announceRankShift(player, change);

            }

        }

    }



    public void sendDailyBonus(Player player, int bonus) {

        player.sendMessage("§6§lPRIMEIRA VITÓRIA DO DIA");

        player.sendMessage("§7+§e" + bonus + " rating bónus");

        player.sendTitle("§6§lBÓNUS DIÁRIO", "§e+" + bonus + " rating", 5, 35, 10);

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.5f);

    }



    public void sendQueueJoin(Player player, String modeName, int queueSize) {

        player.sendMessage("§bTierSpace §8| §7Entraste na fila §f" + modeName + "§7.");

        sendActionBar(player, "§b⏳ §7À procura... §8(§f" + queueSize + "§8 na fila)");

        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 1.5f);

    }



    public void sendRequeue(Player player) {

        sendActionBar(player, "§a↻ Voltaste à fila...");

        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 1.6f);

    }



    public void sendQueueStatus(Player player, String modeName, int queueSize,

                                int estimateSeconds, int rating, long waitedSeconds) {

        sendActionBar(player, "§b" + modeName

                + " §8| §fRating: " + rating

                + " §8| §7Fila: " + queueSize

                + " §8| §e~" + estimateSeconds + "s");

    }



    public void sendQueueLeave(Player player) {

        player.sendMessage("§7Saíste da fila TierSpace.");

        sendActionBar(player, "§7Fila cancelada.");

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 0.8f);

    }



    public void sendMatchFound(Player player, Player opponent, int ownRating, int opponentRating) {

        int diff = opponentRating - ownRating;

        String diffLabel = diff >= 0 ? "+" + diff : String.valueOf(diff);

        String opponentRank = TierRankUtil.fromRating(opponentRating).displayName();



        player.sendMessage("§a§lMATCH ENCONTRADO §8| §fvs " + opponent.getName());

        player.sendTitle("§e§lMATCH!", "§7vs §f" + opponent.getName(), 5, 35, 8);

        player.sendMessage("§7Rank oponente: " + opponentRank);

        player.sendMessage("§7Rating: §f" + ownRating + " vs " + opponentRating

                + " §8(§e" + diffLabel + " diff§8)");

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.9f, 0.8f);

        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.7f, 1.2f);

    }



    public void sendCombatBar(Player player, double ownHp, double oppHp, String modeName,

                              int rating, int streak, String rankLabel) {

        sendActionBar(player, "§b" + modeName

                + " §8| §f" + rankLabel

                + " §8| §e" + rating

                + " §8| §c⚔ " + streak

                + " §8| §c❤ " + formatHp(ownHp)

                + " §8/ §c" + formatHp(oppHp));

    }



    private String formatHp(double hp) {

        return String.format("%.1f", hp);

    }



    private void playWinCelebration(Player player) {

        player.getWorld().spawnParticle(Particle.TOTEM, player.getLocation().add(0, 1, 0), 25, 0.5, 0.7, 0.5, 0.05);

        player.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, player.getLocation().add(0, 1, 0), 12, 0.3, 0.4, 0.3, 0.02);

        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.8f);

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.4f);

    }



    public void sendCountdown(Player player, int secondsLeft) {

        String color = switch (secondsLeft) {

            case 3 -> "§e";

            case 2 -> "§6";

            case 1 -> "§c";

            default -> "§f";

        };

        player.sendTitle(color + "§l" + secondsLeft, "§7Prepara-te...", 0, 22, 0);

        float pitch = 0.8f + (0.2f * (4 - secondsLeft));

        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.7f, pitch);



        if (secondsLeft == 1) {

            player.getWorld().spawnParticle(Particle.CRIT, player.getLocation().add(0, 1, 0),

                    6, 0.3, 0.2, 0.3, 0.01);

        }

    }



    public void sendFightStart(Player player) {

        player.sendTitle("§c§lFIGHT!", "", 0, 18, 6);

        player.playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 0.5f, 1.4f);

        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.25f, 1.6f);

        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation().add(0, 0.5, 0),

                8, 0.2, 0.1, 0.2, 0.01);

        sendActionBar(player, "§c⚔ Boa sorte!");

    }



    private void announceRankShift(Player player, RatingChange change) {

        if (change.promoted()) {

            player.sendMessage("§6§lPROMOÇÃO §7→ " + change.newRank().displayName());

            player.sendTitle("§6§lPROMOÇÃO!", change.newRank().displayName(), 5, 55, 12);

            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.6f);

            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);

            player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation().add(0, 1.5, 0),

                    15, 0.4, 0.5, 0.4, 0.02);

        } else if (change.demoted()) {

            player.sendMessage("§8§lDESPROMOÇÃO §7→ " + change.newRank().displayName());

            player.sendTitle("§8§lDESPROMOÇÃO", change.newRank().displayName(), 5, 40, 10);

            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.4f, 0.8f);

        }

    }



    private void sendProgressLine(Player player, RatingChange change) {

        player.sendMessage("§7" + formatSignedDelta(change.delta()) + " rating → "

                + TierRankUtil.formatProgress(change.newRating())

                + " §8(" + TierRankUtil.formatNextRankLine(change.newRating()) + "§8)");

    }



    private String buildWinSubtitle(RatingChange change) {

        if (change.hadStreakBonus()) {

            return "§a+" + change.delta() + " rating §8(§e×" + change.streakMultiplierLabel() + " streak§8)";

        }

        return "§a+" + change.delta() + " rating";

    }



    private String buildWinActionBar(RatingChange change, boolean inPlacement) {

        StringBuilder bar = new StringBuilder();

        bar.append("§a+").append(change.delta()).append(" rating");

        if (change.hadStreakBonus()) {

            bar.append(" §8(§e×").append(change.streakMultiplierLabel()).append(" streak§8)");

        }

        if (inPlacement) {

            bar.append(" §8| §ePlacement");

        } else {

            bar.append(" §8| §f").append(change.newRank().displayName());

        }

        bar.append(" §8| §c⚔ ").append(change.newStreak()).append(" streak");

        return bar.toString();

    }



    private String formatSignedDelta(int delta) {

        return delta >= 0 ? "+" + delta : String.valueOf(delta);

    }



    public void sendActionBar(Player player, String message) {

        try {

            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));

        } catch (NoClassDefFoundError | NoSuchMethodError ignored) {

            player.sendMessage(message);

        }

    }

}

