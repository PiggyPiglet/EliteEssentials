package com.eliteessentials.services;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing command cooldowns.
 */
public class CooldownService {

    // Map of command name -> (player UUID -> expiry timestamp)
    private final Map<String, Map<UUID, Long>> cooldowns = new ConcurrentHashMap<>();

    /**
     * Check if a player is on cooldown for a command.
     * 
     * @param command Command name
     * @param playerId Player UUID
     * @return Seconds remaining, or 0 if not on cooldown
     */
    public int getCooldownRemaining(String command, UUID playerId) {
        Map<UUID, Long> commandCooldowns = cooldowns.get(command);
        if (commandCooldowns == null) return 0;
        
        Long expiryTime = commandCooldowns.get(playerId);
        if (expiryTime == null) return 0;
        
        long remaining = expiryTime - System.currentTimeMillis();
        if (remaining <= 0) {
            commandCooldowns.remove(playerId);
            return 0;
        }
        
        return (int) Math.ceil(remaining / 1000.0);
    }

    /**
     * Check if a player can use a command (not on cooldown).
     */
    public boolean canUse(String command, UUID playerId) {
        return getCooldownRemaining(command, playerId) == 0;
    }

    /**
     * Set a cooldown for a player on a command.
     * 
     * @param command Command name
     * @param playerId Player UUID
     * @param seconds Cooldown duration in seconds
     */
    public void setCooldown(String command, UUID playerId, int seconds) {
        if (seconds <= 0) return;
        
        cooldowns.computeIfAbsent(command, k -> new ConcurrentHashMap<>())
                 .put(playerId, System.currentTimeMillis() + (seconds * 1000L));
    }

    /**
     * Clear a player's cooldown for a command.
     */
    public void clearCooldown(String command, UUID playerId) {
        Map<UUID, Long> commandCooldowns = cooldowns.get(command);
        if (commandCooldowns != null) {
            commandCooldowns.remove(playerId);
        }
    }

    /**
     * Clear all cooldowns for a player.
     */
    public void clearAllCooldowns(UUID playerId) {
        for (Map<UUID, Long> commandCooldowns : cooldowns.values()) {
            commandCooldowns.remove(playerId);
        }
    }
}
