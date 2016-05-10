package com.j0ach1mmall3.kfa.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ReflectionUtil {
    private static final String VERSION;

    private static Field connectionField;
    private static Method handleMethod;
    private static Method sendMethod;

    static {
        VERSION = getVersion();

        Class<?> craftPlayer = getNmsClass("EntityPlayer");
        try {
            connectionField = craftPlayer.getField("playerConnection");
            handleMethod = craftPlayer.getMethod("getHandle");
            sendMethod = craftPlayer.getMethod("sendPacket");
        } catch (NoSuchMethodException | NoSuchFieldException ex) {
            System.out.printf("[KFA] %s%n", ex.getMessage());
        }
    }

    public static String getVersion() {
        String[] array = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",");
        if (array.length == 4)
            return array[3] + ".";
        return "";
    }

    public static Class<?> getNmsClass(String name) {
        String className = new StringBuilder("net.minecraft.server.")
                .append(VERSION)
                .append(name)
                .toString();

        Class<?> clazz = null;
        try {
            clazz = Class.forName(className);
        } catch (ClassNotFoundException ex) {
            getLogger().log(Level.WARNING, "Class not found: ", ex);
        }
        return clazz;
    }

    public static Object getHandle(Player player) {
        try {
            return handleMethod.invoke(player);
        } catch (Exception ex) {
            getLogger().log(Level.WARNING, "Failed to get handle: ", ex);
            return null;
        }
    }

    public static void sendPacket(Player player, Object packet) {
        try {
            sendMethod.invoke(connectionField.get(ReflectionUtil.getHandle(player)), packet);
        } catch (IllegalAccessException | InvocationTargetException ex) {
            getLogger().log(Level.WARNING, "Failed to send packet: ", ex);
        }
    }

    private static Logger getLogger() {
        return JavaPlugin.getProvidingPlugin(ReflectionUtil.class).getLogger();
    }
}
