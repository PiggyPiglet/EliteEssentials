package com.eliteessentials.services;

import com.eliteessentials.config.ConfigManager;
import com.eliteessentials.model.PlayerFile;
import com.eliteessentials.storage.PlayerFileStorage;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Service for managing player data.
 * Tracks player sessions and updates play time on disconnect.
 */
public class PlayerService {

    private static final Logger logger = Logger.getLogger("EliteEssentials");
    
    private final PlayerFileStorage storage;
    private final ConfigManager configManager;
    
    // Track session start times for play time calculation
    private final Map<UUID, Long> sessionStartTimes = new ConcurrentHashMap<>();

    public PlayerService(PlayerFileStorage storage, ConfigManager configManager) {
        this.storage = storage;
        this.configManager = configManager;
    }

    /**
     * Called when a player joins the server.
     * Creates or updates their player data.
     */
    public PlayerFile onPlayerJoin(UUID playerId, String playerName) {
        boolean isNew = !storage.hasPlayer(playerId);
        PlayerFile data = storage.getPlayer(playerId, playerName);
        
        // Update name in case it changed
        if (!data.getName().equals(playerName)) {
            logger.info("Player " + playerId + " name changed: " + data.getName() + " -> " + playerName);
            data.setName(playerName);
            storage.markDirty(playerId);
        }
        
        // Set starting balance for new players if economy is enabled
        if (isNew && configManager.getConfig().economy.enabled) {
            double startingBalance = configManager.getConfig().economy.startingBalance;
            if (startingBalance > 0) {
                data.setWallet(startingBalance);
                storage.markDirty(playerId);
                logger.info("Set starting balance of " + startingBalance + " for new player " + playerName);
            }
        }
        
        // Track session start
        sessionStartTimes.put(playerId, System.currentTimeMillis());
        
        // Save if new player
        if (isNew) {
            storage.savePlayer(playerId);
        }
        
        return data;
    }

    /**
     * Called when a player leaves the server.
     * Updates last seen and play time.
     */
    public void onPlayerQuit(UUID playerId) {
        PlayerFile data = storage.getPlayer(playerId);
        if (data != null) {
            // Update last seen
            data.updateLastSeen();
            
            // Calculate and add session play time
            Long sessionStart = sessionStartTimes.remove(playerId);
            if (sessionStart != null) {
                long sessionSeconds = (System.currentTimeMillis() - sessionStart) / 1000;
                data.addPlayTime(sessionSeconds);
            }
            
            storage.markDirty(playerId);
        }
        
        // Unload player (saves if dirty)
        storage.unloadPlayer(playerId);
    }

    /**
     * Check if this is a player's first time joining.
     */
    public boolean isFirstJoin(UUID playerId) {
        return !storage.hasPlayer(playerId);
    }

    /**
     * Get player data by UUID.
     */
    public Optional<PlayerFile> getPlayer(UUID playerId) {
        return Optional.ofNullable(storage.getPlayer(playerId));
    }

    /**
     * Get player data by name.
     */
    public Optional<PlayerFile> getPlayerByName(String name) {
        return Optional.ofNullable(storage.getPlayerByName(name));
    }

    /**
     * Get wallet balance for a player.
     */
    public double getBalance(UUID playerId) {
        PlayerFile data = storage.getPlayer(playerId);
        return data != null ? data.getWallet() : 0.0;
    }

    /**
     * Add money to a player's wallet.
     */
    public boolean addMoney(UUID playerId, double amount) {
        PlayerFile data = storage.getPlayer(playerId);
        if (data == null) {
            return false;
        }
        
        data.modifyWallet(amount);
        storage.saveAndMarkDirty(playerId);
        return true;
    }

    /**
     * Remove money from a player's wallet.
     * Returns false if insufficient funds.
     */
    public boolean removeMoney(UUID playerId, double amount) {
        PlayerFile data = storage.getPlayer(playerId);
        if (data == null) {
            return false;
        }
        
        if (!data.modifyWallet(-amount)) {
            return false;  // Insufficient funds
        }
        
        storage.saveAndMarkDirty(playerId);
        return true;
    }

    /**
     * Set a player's wallet balance directly.
     */
    public boolean setBalance(UUID playerId, double amount) {
        PlayerFile data = storage.getPlayer(playerId);
        if (data == null) {
            return false;
        }
        
        data.setWallet(amount);
        storage.saveAndMarkDirty(playerId);
        return true;
    }

    /**
     * Get total unique player count.
     */
    public int getTotalPlayerCount() {
        return storage.getPlayerCount();
    }

    /**
     * Get all players sorted by last seen.
     */
    public List<PlayerFile> getRecentPlayers(int limit) {
        List<PlayerFile> players = storage.getPlayersByLastSeen();
        return players.subList(0, Math.min(limit, players.size()));
    }

    /**
     * Get top players by play time.
     */
    public List<PlayerFile> getTopByPlayTime(int limit) {
        List<PlayerFile> players = storage.getPlayersByPlayTime();
        return players.subList(0, Math.min(limit, players.size()));
    }

    /**
     * Get top players by wallet balance.
     */
    public List<PlayerFile> getTopByBalance(int limit) {
        List<PlayerFile> players = storage.getPlayersByWallet();
        return players.subList(0, Math.min(limit, players.size()));
    }

    /**
     * Format play time as human-readable string.
     */
    public static String formatPlayTime(long seconds) {
        if (seconds < 60) {
            return seconds + "s";
        }
        
        long minutes = seconds / 60;
        if (minutes < 60) {
            return minutes + "m";
        }
        
        long hours = minutes / 60;
        minutes = minutes % 60;
        if (hours < 24) {
            return hours + "h " + minutes + "m";
        }
        
        long days = hours / 24;
        hours = hours % 24;
        return days + "d " + hours + "h";
    }

    /**
     * Save all player data.
     */
    public void save() {
        storage.saveAll();
    }

    /**
     * Reload player data from disk.
     */
    public void reload() {
        storage.reload();
    }
    
    /**
     * Get the underlying storage (for migration and advanced operations).
     */
    public PlayerFileStorage getStorage() {
        return storage;
    }
}
