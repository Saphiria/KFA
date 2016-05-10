package com.j0ach1mmall3.kfa;

import com.j0ach1mmall3.kfa.event.PlayerListener;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Level;

public final class KFAPlugin extends JavaPlugin {
    private ResourceBundle resourceBundle;
    private List<String> broadcastMessages;
    private boolean broadcastEnabled;
    private Listener playerListener;
    private int countdownDuration;
    private int requiredPlayers;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        try {
            this.resourceBundle = ResourceBundle.getBundle("i18n/messages");
        } catch (MissingResourceException ex) {
            this.resourceBundle = ResourceBundle.getBundle("i18n/messages", Locale.ENGLISH);
            getLogger().log(Level.INFO, "Sorry, KFA has not yet been translated into your language!");
            getLogger().log(Level.INFO, "If you wish to help translate it please visit this link:");
            getLogger().log(Level.INFO, "https://github.com/j0ach1mmall3/KFA");
        }

        playerListener = new PlayerListener(this);
        configure();
        getServer().getPluginManager().registerEvents(playerListener, this);
    }

    private void configure() {
        broadcastMessages = getConfig().getStringList("broadcast.messages");
        broadcastEnabled = getConfig().getBoolean("broadcast.enable", true);
        countdownDuration = getConfig().getInt("countdown", 5);
        requiredPlayers = getConfig().getInt("required", 2);

        if(broadcastMessages.isEmpty() && broadcastEnabled) {
            broadcastEnabled = false;
        }

        if (countdownDuration < 1) {
            countdownDuration = 3;
        }

        if (requiredPlayers < 2) {
            requiredPlayers = 2;
        }
    }

    @Override
    public void onDisable() {
        if(playerListener != null) {
            PlayerCommandPreprocessEvent.getHandlerList().unregister(playerListener);
            AsyncPlayerChatEvent.getHandlerList().unregister(playerListener);
            PlayerLoginEvent.getHandlerList().unregister(playerListener);
            PlayerJoinEvent.getHandlerList().unregister(playerListener);
            PlayerQuitEvent.getHandlerList().unregister(playerListener);
            PlayerMoveEvent.getHandlerList().unregister(playerListener);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        reloadConfig();
        configure();
        sender.sendMessage(ChatColor.GREEN.toString().concat("Configuration has been reloaded!"));
        return true;
    }

    public String getTranslation(String key, Object... params) {
        String message = new StringBuilder("<translation \"").append(key).append("\" is missing>").toString();
        try {
            message = resourceBundle.getString(key);
            if (params != null && params.length > 0) {
                message = MessageFormat.format(message, params);
            }
        } catch (MissingResourceException ex) {
            // fall through
        }
        return message;
    }

    public void restart() {
        if (getServer().getOnlinePlayers().size() > 0) {
            for (Player player : getServer().getOnlinePlayers()) {
                player.kickPlayer(getTranslation("game_restart"));
            }
        }

        getServer().getScheduler().cancelTasks(this);
        onDisable();
        playerListener = new PlayerListener(this);
        getServer().getPluginManager().registerEvents(playerListener, this);
    }

    public int getRequiredPlayers() {
        return requiredPlayers;
    }

    public int getCountdownDuration() {
        return countdownDuration;
    }

    public List<String> getBroadcastMessages() {
        return broadcastMessages;
    }

    public boolean isBroadcastEnabled() {
        return broadcastEnabled;
    }
}
