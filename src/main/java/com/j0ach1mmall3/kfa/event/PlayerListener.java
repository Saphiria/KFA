package com.j0ach1mmall3.kfa.event;

import com.j0ach1mmall3.kfa.KFAPlugin;
import com.j0ach1mmall3.kfa.util.Announcer;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author Connor Spencer Harries
 */
public final class PlayerListener implements Listener {
    private final Set<UUID> kickedPlayerIds;
    private final Set<Player> joined;
    private final KFAPlugin plugin;

    private Scoreboard scoreboard;
    private Objective objective;
    private boolean running;
    private int scoreTaskId;

    public PlayerListener(KFAPlugin plugin) {
        this.kickedPlayerIds = new HashSet<>();
        this.joined = new HashSet<>();
        this.plugin = plugin;
    }

    @EventHandler
    public void onLogin(PlayerLoginEvent event) {
        if (!running) {
            return;
        }

        Player player = event.getPlayer();
        if (kickedPlayerIds.contains(player.getUniqueId())) {
            event.setResult(PlayerLoginEvent.Result.KICK_BANNED);
            event.setKickMessage(plugin.getTranslation("player_banned", player.getName()));
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        joined.add(event.getPlayer());
        event.setJoinMessage("");

        Player player = event.getPlayer();
        delayMessage(player, plugin.getTranslation("player_join", player.getName()));
        if (joined.size() == plugin.getRequiredPlayers()) {
            String scoreTitle = plugin.getTranslation("scoreboard_score");
            scoreboard = plugin.getServer().getScoreboardManager().getNewScoreboard();
            objective = this.scoreboard.registerNewObjective(scoreTitle, "dummy");
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);

            /*
             * TODO: Use ProtocolLib or TinyProtocol to send sounds to players individually
             */
            for (Player gamePlayer : joined) {
                gamePlayer.playSound(gamePlayer.getLocation(), Sound.ENDERDRAGON_GROWL, 10f, 1f);
                objective.getScore(gamePlayer.getName()).setScore(0);
            }

            delayMessage(player, plugin.getTranslation("game_warning", player.getName()));
            BukkitRunnable announcer = new Announcer(plugin, joined, new Runnable() {
                @Override
                public void run() {
                    running = true;
                    String message = plugin.getTranslation("game_begin");
                    plugin.getServer().broadcastMessage(message);
                    for (Player gamePlayer : joined) {
                        gamePlayer.playSound(gamePlayer.getLocation(), Sound.NOTE_PIANO, 1f, 1f);
                        gamePlayer.setScoreboard(scoreboard);
                    }
                    scoreTaskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
                        @Override
                        public void run() {
                            for (Player gamePlayer : joined) {
                                Score score = objective.getScore(gamePlayer.getName());
                                score.setScore(score.getScore() + 1);
                                if (score.getScore() % 10 == 0) {
                                    Location location = gamePlayer.getLocation();
                                    gamePlayer.playSound(location, Sound.NOTE_PIANO, 1f, 1f);
                                }

                                if (!plugin.isBroadcastEnabled()) {
                                    continue;
                                }

                                if (score.getScore() % 4 == 0 && ThreadLocalRandom.current().nextBoolean()) {
                                    int index = ThreadLocalRandom.current().nextInt(plugin.getBroadcastMessages().size());
                                    String message = plugin.getBroadcastMessages().get(index);
                                    gamePlayer.sendMessage(formatMessage(gamePlayer, message));
                                }
                            }
                        }
                    }, 20L, 20L);
                }
            });
            announcer.runTaskTimer(plugin, 100L, 20L);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        joined.remove(player);
        if (!running) {
            return;
        }
        event.setQuitMessage(plugin.getTranslation("player_kicked", player.getName()));
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!running) {
            return;
        }

        Player player = event.getPlayer();
        if (player.hasPermission("kfa.bypass.look") && player.hasPermission("kfa.bypass.move")) {
            return;
        }

        Location from = event.getFrom();
        Location to = event.getTo();
        event.setCancelled(true);

        if (!samePosition(from, to) && !player.hasPermission("kfa.bypass.move")) {
            handleLoss(player);
            return;
        }

        if (!sameDirection(from, to) && !player.hasPermission("kfa.bypass.look")) {
            handleLoss(player);
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        if (!running || event.getPlayer().hasPermission("kfa.bypass.chat")) {
            return;
        }
        handleLoss(event.getPlayer());
        event.setCancelled(true);
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!running || event.getPlayer().hasPermission("kfa.bypass.command")) {
            return;
        }
        handleLoss(event.getPlayer());
        event.setCancelled(true);
    }

    private String formatMessage(Player player, String message) {
        if (message.contains("#{online_player}")) {
            int index = ThreadLocalRandom.current().nextInt(joined.size());
            Player formatPlayer = getPlayerFromSet(index);
            // Prevent sending the player their own name.
            int iterations = 0;
            while (formatPlayer.getName().equals(player.getName()) && iterations++ < 5) {
                index = ThreadLocalRandom.current().nextInt(joined.size());
                formatPlayer = getPlayerFromSet(index);
            }
            message = message.replace("#{online_player}", formatPlayer.getName());
        }
        return message;
    }

    private Player getPlayerFromSet(int index) {
        Player[] players = (Player[]) joined.toArray();
        return players[index];
    }

    private boolean samePosition(Location from, Location to) {
        if (Double.compare(from.getX(), to.getBlockX()) != 0) {
            return false;
        }
        if (Double.compare(from.getY(), to.getY()) != 0) {
            return false;
        }
        return Double.compare(from.getZ(), to.getZ()) == 0;
    }

    private boolean sameDirection(Location from, Location to) {
        if (Float.compare(from.getYaw(), to.getYaw()) != 0) {
            return false;
        }
        return Float.compare(from.getPitch(), to.getPitch()) == 0;
    }

    private void handleLoss(Player player) {
        kickedPlayerIds.add(player.getUniqueId());
        joined.remove(player);

        player.kickPlayer(plugin.getTranslation("player_lose", player.getName()));

        if (joined.size() == 1) {
            Player winner = joined.iterator().next();
            String message = plugin.getTranslation("player_win", winner.getName());
            winner.playSound(winner.getLocation(), Sound.LEVEL_UP, 10f, 1f);
            scoreboard.clearSlot(DisplaySlot.SIDEBAR);
            plugin.getServer().broadcastMessage(message);
            plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
                @Override
                public void run() {
                    plugin.getServer().broadcastMessage(plugin.getTranslation("game_restart"));
                    plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
                        @Override
                        public void run() {
                            plugin.restart();
                        }
                    }, 100L);
                }
            }, 20L);
            plugin.getServer().getScheduler().cancelTask(scoreTaskId);
            running = false;
        }
    }

    private void delayMessage(final Player player, final String message) {
        if (message.isEmpty()) {
            return;
        }

        plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                player.sendMessage(message);
            }
        }, 20L);
    }
}
