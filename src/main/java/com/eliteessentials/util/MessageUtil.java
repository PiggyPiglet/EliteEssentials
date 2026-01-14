package com.eliteessentials.util;

import com.eliteessentials.EliteEssentials;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for message formatting and color codes.
 */
public final class MessageUtil {

    private static final Pattern COLOR_PATTERN = Pattern.compile("&([0-9a-fk-or])");
    
    // Hytale may use a different color code system - this is a placeholder
    // that can be adapted once the actual API is known
    private static final String COLOR_CHAR = "\u00A7"; // Section sign, common in MC-like games

    private MessageUtil() {
        // Utility class
    }

    /**
     * Get a message from the config with prefix.
     */
    public static String getMessage(String key) {
        EliteEssentials plugin = EliteEssentials.getInstance();
        if (plugin == null || plugin.getConfigManager() == null) {
            return "&cMessage not found: " + key;
        }
        
        String prefix = plugin.getConfigManager().getPrefix();
        String message = plugin.getConfigManager().getMessage(key);
        
        return colorize(prefix + message);
    }

    /**
     * Get a raw message from the config without prefix.
     */
    public static String getRawMessage(String key) {
        EliteEssentials plugin = EliteEssentials.getInstance();
        if (plugin == null || plugin.getConfigManager() == null) {
            return "&cMessage not found: " + key;
        }
        
        return colorize(plugin.getConfigManager().getMessage(key));
    }

    /**
     * Convert color codes (&a, &b, etc.) to the game's format.
     */
    public static String colorize(String message) {
        if (message == null) return "";
        
        Matcher matcher = COLOR_PATTERN.matcher(message);
        StringBuilder sb = new StringBuilder();
        
        while (matcher.find()) {
            matcher.appendReplacement(sb, COLOR_CHAR + matcher.group(1));
        }
        matcher.appendTail(sb);
        
        return sb.toString();
    }

    /**
     * Strip color codes from a message.
     */
    public static String stripColors(String message) {
        if (message == null) return "";
        return message.replaceAll("&[0-9a-fk-or]", "")
                     .replaceAll(COLOR_CHAR + "[0-9a-fk-or]", "");
    }

    /**
     * Format a message with placeholders.
     * 
     * @param message Message with {placeholder} syntax
     * @param replacements Key-value pairs: "placeholder", "value", ...
     * @return Formatted message
     */
    public static String format(String message, String... replacements) {
        if (message == null) return "";
        if (replacements == null || replacements.length < 2) return message;
        
        String result = message;
        for (int i = 0; i < replacements.length - 1; i += 2) {
            String placeholder = replacements[i];
            String value = replacements[i + 1];
            result = result.replace("{" + placeholder + "}", value != null ? value : "");
        }
        
        return result;
    }

    /**
     * Send a formatted message to a player.
     * This is a placeholder - actual implementation depends on Hytale API.
     * 
     * @param message The message to format and send
     * @return The colorized message (caller handles actual sending)
     */
    public static String prepareMessage(String message) {
        return colorize(message);
    }
}
