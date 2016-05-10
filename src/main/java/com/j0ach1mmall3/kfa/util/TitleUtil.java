package com.j0ach1mmall3.kfa.util;

import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;

@SuppressWarnings("unchecked")
public class TitleUtil {
    private static Constructor<?> packetConstructor;
    private static Class<Enum> enumTitleAction;
    private static Method method;

    static {
        enumTitleAction = (Class<Enum>) ReflectionUtil.getNmsClass("PacketPlayOutTitle$EnumTitleAction");
        Class<?> serializerClass = ReflectionUtil.getNmsClass("IChatBaseComponent$ChatSerializer");
        Class<?> component = ReflectionUtil.getNmsClass("IChatBaseComponent");
        Class<?> packet = ReflectionUtil.getNmsClass("PacketPlayOutTitle");

        Class[] params = new Class[]{
                enumTitleAction,
                component,
                int.class,
                int.class,
                int.class
        };
        try {
            method = serializerClass.getMethod("a", String.class);
            packetConstructor = packet.getConstructor(params);
        } catch (NoSuchMethodException ex) {
            System.out.printf("[KFA] %s%n", ex.getMessage());
        }
    }

    public static void send(Player player, int fadeDuration, int displayDuration, TitleType type, String message) {
        try {
            Object serialized = method.invoke(null, "{\"text\": \"" + message + "\"}");
            Object[] params = new Object[]{
                    enumTitleAction.getEnumConstants()[type.ordinal()],
                    serialized,
                    fadeDuration,
                    displayDuration,
                    fadeDuration
            };
            Object packet = packetConstructor.newInstance(params);
            ReflectionUtil.sendPacket(player, packet);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | InstantiationException ex) {
            player.getServer().getLogger().log(Level.WARNING, "Failed to send title: ", ex);
        }
    }

    public enum TitleType {
        TITLE,
        SUBTITLE;
    }
}
