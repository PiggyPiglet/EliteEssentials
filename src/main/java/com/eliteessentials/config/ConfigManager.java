package com.eliteessentials.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

/**
 * Manages plugin configuration loading and access.
 */
public class ConfigManager {

    private static final Logger logger = Logger.getLogger("EliteEssentials");
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    private final File dataFolder;
    private PluginConfig config;

    public ConfigManager(File dataFolder) {
        this.dataFolder = dataFolder;
    }

    public void loadConfig() {
        // Create the plugin folder if it doesn't exist
        if (!dataFolder.exists()) {
            if (dataFolder.mkdirs()) {
                logger.info("Created plugin folder: " + dataFolder.getAbsolutePath());
            } else {
                logger.warning("Could not create plugin folder: " + dataFolder.getAbsolutePath());
            }
        }
        
        File configFile = new File(dataFolder, "config.json");
        
        // Create default config if it doesn't exist
        if (!configFile.exists()) {
            logger.info("Config file not found, creating default config.json...");
            saveDefaultConfig(configFile);
        }
        
        // Load the config
        try (Reader reader = new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8)) {
            config = gson.fromJson(reader, PluginConfig.class);
            if (config == null) {
                config = new PluginConfig();
            }
            logger.info("Configuration loaded from: " + configFile.getAbsolutePath());
        } catch (Exception e) {
            logger.severe("Failed to load config.json: " + e.getMessage());
            config = new PluginConfig(); // Use defaults
        }
    }

    public void saveConfig() {
        // Ensure folder exists
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        
        File configFile = new File(dataFolder, "config.json");
        
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(configFile), StandardCharsets.UTF_8)) {
            gson.toJson(config, writer);
            logger.info("Configuration saved to: " + configFile.getAbsolutePath());
        } catch (Exception e) {
            logger.severe("Failed to save config.json: " + e.getMessage());
        }
    }

    private void saveDefaultConfig(File configFile) {
        // Ensure folder exists
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        
        // Try to copy from JAR resources first (bundled default config)
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("config.json")) {
            if (in != null) {
                try (OutputStream out = new FileOutputStream(configFile)) {
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = in.read(buffer)) > 0) {
                        out.write(buffer, 0, length);
                    }
                }
                logger.info("Default config.json created at: " + configFile.getAbsolutePath());
                return;
            }
        } catch (Exception e) {
            logger.warning("Could not copy default config from JAR: " + e.getMessage());
        }
        
        // Fallback: create default config programmatically from PluginConfig defaults
        logger.info("Creating default config programmatically...");
        config = new PluginConfig();
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(configFile), StandardCharsets.UTF_8)) {
            gson.toJson(config, writer);
            logger.info("Default config.json created at: " + configFile.getAbsolutePath());
        } catch (Exception e) {
            logger.severe("Failed to create default config: " + e.getMessage());
        }
    }

    public PluginConfig getConfig() {
        return config;
    }

    // Convenience methods for common config access
    
    public int getRtpMinRange() {
        return config.rtp.minRange;
    }

    public int getRtpMaxRange() {
        return config.rtp.maxRange;
    }

    public int getRtpCooldown() {
        return config.rtp.cooldownSeconds;
    }

    public int getRtpMaxAttempts() {
        return config.rtp.maxAttempts;
    }

    public int getBackMaxHistory() {
        return config.back.maxHistory;
    }

    public boolean isBackOnDeathEnabled() {
        return config.back.workOnDeath;
    }

    public int getTpaTimeout() {
        return config.tpa.timeoutSeconds;
    }

    public int getMaxHomes() {
        return config.homes.maxHomes;
    }

    public String getMessage(String key) {
        return config.messages.getOrDefault(key, "&cMissing message: " + key);
    }

    public String getPrefix() {
        return config.messages.getOrDefault("prefix", "&7[&bEliteEssentials&7] ");
    }

    public boolean isDebugEnabled() {
        return config.debug;
    }
}
