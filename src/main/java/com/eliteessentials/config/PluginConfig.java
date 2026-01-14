package com.eliteessentials.config;

import java.util.HashMap;
import java.util.Map;

/**
 * POJO representing the plugin configuration structure.
 * Loaded from config.json via Gson.
 * 
 * This file is saved to: mods/EliteEssentials/config.json
 * Server owners can edit this file to customize all settings.
 */
public class PluginConfig {

    // ==================== GENERAL ====================
    
    /** Enable debug logging (verbose output for troubleshooting) */
    public boolean debug = false;

    // ==================== COMMAND CONFIGS ====================
    
    public RtpConfig rtp = new RtpConfig();
    public BackConfig back = new BackConfig();
    public TpaConfig tpa = new TpaConfig();
    public HomesConfig homes = new HomesConfig();
    public SpawnConfig spawn = new SpawnConfig();
    public SleepConfig sleep = new SleepConfig();
    
    // ==================== MESSAGES ====================
    
    public Map<String, String> messages = new HashMap<>();

    public PluginConfig() {
        initDefaultMessages();
    }
    
    private void initDefaultMessages() {
        messages.put("prefix", "[EliteEssentials] ");
        messages.put("noPermission", "You don't have permission to use this command.");
        messages.put("playerNotFound", "Player not found.");
        messages.put("commandDisabled", "This command is disabled.");
        messages.put("onCooldown", "You must wait {seconds} seconds before using this command again.");
        messages.put("warmupStarted", "Teleporting in {seconds} seconds. Don't move!");
        messages.put("warmupCancelled", "Teleport cancelled - you moved!");
    }

    // ==================== RTP (Random Teleport) ====================
    
    public static class RtpConfig {
        /** Enable/disable the /rtp command */
        public boolean enabled = true;
        
        /** Minimum distance from player for random location */
        public int minRange = 100;
        
        /** Maximum distance from player for random location */
        public int maxRange = 5000;
        
        /** Cooldown in seconds between uses (0 = no cooldown) */
        public int cooldownSeconds = 30;
        
        /** Warmup in seconds - player must stand still (0 = instant) */
        public int warmupSeconds = 3;
        
        /** Max attempts to find a safe location before giving up */
        public int maxAttempts = 10;
        
        /** Minimum Y level - rejects locations below this (avoid dungeons) */
        public int minSurfaceY = 50;
    }

    // ==================== BACK ====================
    
    public static class BackConfig {
        /** Enable/disable the /back command */
        public boolean enabled = true;
        
        /** How many previous locations to remember per player */
        public int maxHistory = 5;
        
        /** Save location on death (allows /back to death point) */
        public boolean workOnDeath = true;
        
        /** Cooldown in seconds between uses (0 = no cooldown) */
        public int cooldownSeconds = 0;
        
        /** Warmup in seconds - player must stand still (0 = instant) */
        public int warmupSeconds = 0;
    }

    // ==================== TPA (Teleport Ask) ====================
    
    public static class TpaConfig {
        /** Enable/disable /tpa, /tpaccept, /tpdeny commands */
        public boolean enabled = true;
        
        /** Seconds before a TPA request expires */
        public int timeoutSeconds = 30;
        
        /** Warmup in seconds after accepting - requester must stand still (0 = instant) */
        public int warmupSeconds = 3;
    }

    // ==================== HOMES ====================
    
    public static class HomesConfig {
        /** Enable/disable /home, /sethome, /delhome, /homes commands */
        public boolean enabled = true;
        
        /** Maximum homes per player */
        public int maxHomes = 3;
        
        /** Default max homes for new players */
        public int defaultMaxHomes = 3;
        
        /** Cooldown in seconds between /home uses (0 = no cooldown) */
        public int cooldownSeconds = 0;
        
        /** Warmup in seconds - player must stand still (0 = instant) */
        public int warmupSeconds = 3;
    }

    // ==================== SPAWN ====================
    
    public static class SpawnConfig {
        /** Enable/disable the /spawn command */
        public boolean enabled = true;
        
        /** Cooldown in seconds between uses (0 = no cooldown) */
        public int cooldownSeconds = 0;
        
        /** Warmup in seconds - player must stand still (0 = instant) */
        public int warmupSeconds = 3;
    }

    // ==================== SLEEP (Night Skip) ====================
    
    public static class SleepConfig {
        /** Enable/disable the sleep percentage feature */
        public boolean enabled = true;
        
        /** Percentage of players that must sleep to skip night (0-100) */
        public int sleepPercentage = 50;
    }
}
