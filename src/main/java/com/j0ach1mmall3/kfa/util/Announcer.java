package com.j0ach1mmall3.kfa.util;

import com.j0ach1mmall3.kfa.KFAPlugin;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Set;

/**
 * @author Connor Spencer Harries
 */
public class Announcer extends BukkitRunnable {
    private final Set<Player> players;
    private final Runnable callback;
    private final KFAPlugin plugin;
    private int duration;

    public Announcer(KFAPlugin plugin, Set<Player> players, Runnable callback) {
        this.duration = plugin.getCountdownDuration();
        this.callback = callback;
        this.players = players;
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (duration == 0) {
            cancel();
            callback.run();
            return;
        }
        String message = plugin.getTranslation("game_countdown", String.valueOf(duration--));
        for (Player player : players) {
            TitleUtil.send(player, 5, 20, TitleUtil.TitleType.TITLE, message);
            player.playSound(player.getLocation(), Sound.CLICK, 1, 1f);
        }
    }
}
