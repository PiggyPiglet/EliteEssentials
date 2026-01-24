package com.eliteessentials.services;

import com.eliteessentials.config.ConfigManager;
import com.eliteessentials.model.PlayerData;
import com.eliteessentials.storage.PlayerStorage;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Service for managing player data cache.
 * Tracks player sessions and updates play time on disconnect.
 */
public class PlayerService {

    private static final Logger logger = Logger.getLogger("EliteEssentials");
    
    private final PlayerStorage storage;
    private final ConfigManager configManager;
    
    // Track session start times for play time calculation
    private final Map<UUID, Long> sessionStartTimes = new ConcurrentHashMap<>();

    public PlayerService(PlayerStorage storage, ConfigManager configManager) {
        this.storage = storage;
        this.configManager = configManager;
    }

    /**
     * Called when a player joins the server.
     * Creates or updates their player data.
     */
    public PlayerData onPlayerJoin(UUID playerId, String playerName) {
        boolean isNew = !storage.hasPlayer(playerId);
        PlayerData data = storage.getOrCreatePlayer(playerId, playerName);
        
        // Update name in case it changed
        if (!data.getName().equals(playerName)) {
            logger.info("Player " + playerId + " name changed: " + data.getName() + " -> " + playerName);
            data.setName(playerName);
        }
        
        // Set starting balance for new players if economy is enabled
        if (isNew && configManager.getConfig().economy.enabled) {
            double startingBalance = configManager.getConfig().economy.startingBalance;
            if (startingBalance > 0) {
                data.setWallet(startingBalance);
                logger.info("Set starting balance of " + startingBalance + " for new player " + playerName);
            }
        }
        
        // Track session start
        sessionStartTimes.put(playerId, System.currentTimeMillis());
        
        return data;
    }

    /**
     * Called when a player leaves the server.
     * Updates last seen and play time.
     */
    public void onPlayerQuit(UUID playerId) {
        storage.getPlayer(playerId).ifPresent(data -> {
            // Update last seen
            data.updateLastSeen();
            
            // Calculate and add session play time
            Long sessionStart = sessionStartTimes.remove(playerId);
            if (sessionStart != null) {
                long sessionSeconds = (System.currentTimeMillis() - sessionStart) / 1000;
                data.addPlayTime(sessionSeconds);
            }
            
            storage.updatePlayer(data);
        });
        
        // Save after player quits
        storage.save();
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
    public Optional<PlayerData> getPlayer(UUID playerId) {
        return storage.getPlayer(playerId);
    }

    /**
     * Get player data by name.
     */
    public Optional<PlayerData> getPlayerByName(String name) {
        return storage.getPlayerByName(name);
    }

    /**
     * Get wallet balance for a player.
     */
    public double getBalance(UUID playerId) {
        return storage.getPlayer(playerId)
                .map(PlayerData::getWallet)
                .orElse(0.0);
    }

    /**
     * Add money to a player's wallet.
     */
    public boolean addMoney(UUID playerId, double amount) {
        Optional<PlayerData> opt = storage.getPlayer(playerId);
        if (opt.isEmpty()) {
            return false;
        }
        
        PlayerData data = opt.get();
        data.modifyWallet(amount);
        storage.updatePlayer(data);
        storage.save();
        return true;
    }

    /**
     * Remove money from a player's wallet.
     * Returns false if insufficient funds.
     */
    public boolean removeMoney(UUID playerId, double amount) {
        Optional<PlayerData> opt = storage.getPlayer(playerId);
        if (opt.isEmpty()) {
            return false;
        }
        
        PlayerData data = opt.get();
        if (!data.modifyWallet(-amount)) {
            return false;  // Insufficient funds
        }
        
        storage.updatePlayer(data);
        storage.save();
        return true;
    }

    /**
     * Set a player's wallet balance directly.
     */
    public boolean setBalance(UUID playerId, double amount) {
        Optional<PlayerData> opt = storage.getPlayer(playerId);
        if (opt.isEmpty()) {
            return false;
        }
        
        PlayerData data = opt.get();
        data.setWallet(amount);
        storage.updatePlayer(data);
        storage.save();
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
    public List<PlayerData> getRecentPlayers(int limit) {
        List<PlayerData> players = storage.getPlayersByLastSeen();
        return players.subList(0, Math.min(limit, players.size()));
    }

    /**
     * Get top players by play time.
     */
    public List<PlayerData> getTopByPlayTime(int limit) {
        List<PlayerData> players = storage.getPlayersByPlayTime();
        return players.subList(0, Math.min(limit, players.size()));
    }

    /**
     * Get top players by wallet balance.
     */
    public List<PlayerData> getTopByBalance(int limit) {
        List<PlayerData> players = storage.getPlayersByWallet();
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
        storage.save();
    }

    /**
     * Reload player data from disk.
     */
    public void reload() {
        storage.load();
    }
}
