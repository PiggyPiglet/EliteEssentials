package com.eliteessentials;

import com.eliteessentials.commands.hytale.*;
import com.eliteessentials.config.ConfigManager;
import com.eliteessentials.services.BackService;
import com.eliteessentials.services.CooldownService;
import com.eliteessentials.services.DeathTrackingService;
import com.eliteessentials.services.HomeService;
import com.eliteessentials.services.RtpService;
import com.eliteessentials.services.SleepService;
import com.eliteessentials.services.TpaService;
import com.eliteessentials.services.WarmupService;
import com.eliteessentials.storage.BackStorage;
import com.eliteessentials.storage.HomeStorage;
import com.eliteessentials.systems.PlayerDeathSystem;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.io.File;
import java.util.logging.Level;

/**
 * EliteEssentials - Essential commands for Hytale servers.
 * 
 * Features:
 * - /home, /sethome, /delhome - Named home locations
 * - /back - Return to previous locations
 * - /rtp - Random teleport
 * - /tpa, /tpaccept, /tpdeny - Teleport requests
 */
public class EliteEssentials extends JavaPlugin {

    private static EliteEssentials instance;
    
    private ConfigManager configManager;
    private HomeStorage homeStorage;
    private BackStorage backStorage;
    private HomeService homeService;
    private BackService backService;
    private TpaService tpaService;
    private RtpService rtpService;
    private SleepService sleepService;
    private WarmupService warmupService;
    private CooldownService cooldownService;
    private DeathTrackingService deathTrackingService;
    private PlayerDeathSystem playerDeathSystem;

    public EliteEssentials(JavaPluginInit init) {
        super(init);
        instance = this;
    }

    @Override
    protected void setup() {
        getLogger().at(Level.INFO).log("EliteEssentials is setting up...");
        
        // Get data folder - Hytale may create mods/Group_Name/ or mods/Group/Name/
        // We want just mods/EliteEssentials/ so construct it directly
        java.nio.file.Path baseDataPath = getDataDirectory();
        File dataFolder;
        
        // Find mods folder in the path
        java.nio.file.Path modsPath = findModsFolder(baseDataPath);
        if (modsPath != null) {
            // Use mods/EliteEssentials/ directly
            dataFolder = modsPath.resolve("EliteEssentials").toFile();
            getLogger().at(Level.INFO).log("Using data folder: " + dataFolder.getAbsolutePath());
        } else {
            // Fallback to what Hytale gave us
            dataFolder = baseDataPath.toFile();
            getLogger().at(Level.WARNING).log("Could not find mods folder, using: " + dataFolder.getAbsolutePath());
        }
        
        // Load configuration
        configManager = new ConfigManager(dataFolder);
        configManager.loadConfig();
        
        // Initialize storage
        homeStorage = new HomeStorage(dataFolder);
        homeStorage.load();
        
        backStorage = new BackStorage(dataFolder);
        backStorage.load();
        
        // Initialize services
        cooldownService = new CooldownService();
        warmupService = new WarmupService();
        homeService = new HomeService(homeStorage, configManager);
        backService = new BackService(configManager, backStorage);
        deathTrackingService = new DeathTrackingService(backService, configManager);
        tpaService = new TpaService(configManager);
        rtpService = new RtpService(configManager);
        sleepService = new SleepService(configManager);
        
        getLogger().at(Level.INFO).log("EliteEssentials setup complete.");
    }

    @Override
    protected void start() {
        getLogger().at(Level.INFO).log("EliteEssentials is starting...");
        
        // Register commands
        registerCommands();
        
        // Register the death tracking ECS system (hooks into Hytale's death events)
        if (configManager.isBackOnDeathEnabled()) {
            try {
                playerDeathSystem = new PlayerDeathSystem(backService);
                EntityStore.REGISTRY.registerSystem(playerDeathSystem);
                getLogger().at(Level.INFO).log("PlayerDeathSystem registered - /back on death enabled!");
            } catch (Exception e) {
                getLogger().at(Level.WARNING).log("Could not register PlayerDeathSystem: " + e.getMessage());
                // Fall back to polling-based death tracking
                if (deathTrackingService != null) {
                    deathTrackingService.start();
                    getLogger().at(Level.INFO).log("Falling back to polling-based death tracking.");
                }
            }
        }
        
        getLogger().at(Level.INFO).log("EliteEssentials started successfully!");
    }

    @Override
    protected void shutdown() {
        getLogger().at(Level.INFO).log("EliteEssentials is shutting down...");
        
        // Unregister the death system
        if (playerDeathSystem != null) {
            try {
                EntityStore.REGISTRY.unregisterSystem(PlayerDeathSystem.class);
            } catch (Exception e) {
                // Ignore unregister errors
            }
        }
        
        // Save all data
        if (homeStorage != null) {
            homeStorage.save();
            getLogger().at(Level.INFO).log("Homes saved.");
        }
        
        if (backService != null) {
            backService.save();
            getLogger().at(Level.INFO).log("Back locations saved.");
        }
        
        // Cleanup services
        if (tpaService != null) {
            tpaService.shutdown();
        }
        if (sleepService != null) {
            sleepService.shutdown();
        }
        if (warmupService != null) {
            warmupService.shutdown();
        }
        if (deathTrackingService != null) {
            deathTrackingService.shutdown();
        }
        
        getLogger().at(Level.INFO).log("EliteEssentials disabled.");
    }
    
    private void registerCommands() {
        // Home commands
        getCommandRegistry().registerCommand(new HytaleHomeCommand(homeService, backService));
        getCommandRegistry().registerCommand(new HytaleSetHomeCommand(homeService));
        getCommandRegistry().registerCommand(new HytaleDelHomeCommand(homeService));
        getCommandRegistry().registerCommand(new HytaleHomesCommand(homeService));
        
        // Back command
        getCommandRegistry().registerCommand(new HytaleBackCommand(backService));
        
        // RTP command
        getCommandRegistry().registerCommand(new HytaleRtpCommand(rtpService, backService, configManager, warmupService));
        
        // TPA commands
        getCommandRegistry().registerCommand(new HytaleTpaCommand(tpaService));
        getCommandRegistry().registerCommand(new HytaleTpAcceptCommand(tpaService, backService));
        getCommandRegistry().registerCommand(new HytaleTpDenyCommand(tpaService));
        
        // Spawn command
        getCommandRegistry().registerCommand(new HytaleSpawnCommand(backService));
        
        // Admin commands (OP only)
        getCommandRegistry().registerCommand(new HytaleSleepPercentCommand(configManager));
        
        getLogger().at(Level.INFO).log("Commands registered: /home, /sethome, /delhome, /homes, /back, /rtp, /tpa, /tpaccept, /tpdeny, /spawn, /sleeppercent");
    }

    public static EliteEssentials getInstance() {
        return instance;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public HomeService getHomeService() {
        return homeService;
    }
    
    public BackService getBackService() {
        return backService;
    }
    
    public TpaService getTpaService() {
        return tpaService;
    }
    
    public RtpService getRtpService() {
        return rtpService;
    }
    
    public WarmupService getWarmupService() {
        return warmupService;
    }
    
    public CooldownService getCooldownService() {
        return cooldownService;
    }
    
    public DeathTrackingService getDeathTrackingService() {
        return deathTrackingService;
    }
    
    /**
     * Find the mods folder in the path hierarchy.
     */
    private java.nio.file.Path findModsFolder(java.nio.file.Path path) {
        java.nio.file.Path current = path;
        while (current != null && current.getNameCount() > 0) {
            String name = current.getFileName().toString();
            if ("mods".equals(name)) {
                return current;
            }
            java.nio.file.Path parent = current.getParent();
            if (parent == null || parent.equals(current)) {
                break;
            }
            current = parent;
        }
        return null;
    }
}
