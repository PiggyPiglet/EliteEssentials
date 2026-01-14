package com.eliteessentials.services;

import com.eliteessentials.config.ConfigManager;
import com.hypixel.hytale.builtin.beds.sleep.components.PlayerSleep;
import com.hypixel.hytale.builtin.beds.sleep.components.PlayerSomnolence;
import com.hypixel.hytale.builtin.beds.sleep.resources.WorldSlumber;
import com.hypixel.hytale.builtin.beds.sleep.resources.WorldSomnolence;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Service that monitors sleeping players and triggers night skip
 * when the configured percentage of players are sleeping.
 * 
 * Integrates with Hytale's built-in sleep system by manipulating
 * the WorldSomnolence resource to trigger WorldSlumber state.
 */
public class SleepService {

    private static final Logger logger = Logger.getLogger("EliteEssentials");

    private final ConfigManager configManager;
    private final ScheduledExecutorService scheduler;
    private volatile boolean slumberTriggered = false;

    public SleepService(ConfigManager configManager) {
        this.configManager = configManager;
        
        // Create scheduler to check sleep status periodically
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "EliteEssentials-SleepCheck");
            t.setDaemon(true);
            return t;
        });
        
        // Check every 1 second
        scheduler.scheduleAtFixedRate(this::checkSleepingPlayers, 5, 1, TimeUnit.SECONDS);
        
        logger.fine("[SleepService] Started with " + configManager.getConfig().sleep.sleepPercentage + "% threshold");
    }

    /**
     * Check all worlds for sleeping players and trigger night skip if threshold is met.
     */
    private void checkSleepingPlayers() {
        if (!configManager.getConfig().sleep.enabled) {
            return;
        }
        
        int requiredPercent = configManager.getConfig().sleep.sleepPercentage;
        
        // If 100%, let vanilla handle it
        if (requiredPercent >= 100) {
            return;
        }
        
        try {
            Universe universe = Universe.get();
            if (universe == null) return;
            
            for (World world : universe.getWorlds().values()) {
                checkWorldSleep(world, requiredPercent);
            }
        } catch (Exception e) {
            // Silently ignore - universe might not be ready
        }
    }

    private void checkWorldSleep(World world, int requiredPercent) {
        try {
            // Get players from Universe for this world
            List<PlayerRef> players = getPlayersInWorld(world);
            
            if (players.isEmpty()) {
                return;
            }
            
            int totalPlayers = players.size();
            int sleepingPlayers = 0;
            
            // Count sleeping players (those in bed, nodding off or in slumber)
            for (PlayerRef player : players) {
                if (isPlayerInBed(player)) {
                    sleepingPlayers++;
                }
            }
            
            if (sleepingPlayers == 0) {
                slumberTriggered = false;
                return;
            }
            
            // Calculate percentage
            int currentPercent = (sleepingPlayers * 100) / totalPlayers;
            
            logger.fine("[SleepService] " + world.getName() + ": " + sleepingPlayers + "/" + totalPlayers + 
                       " sleeping (" + currentPercent + "%, need " + requiredPercent + "%)");
            
            // Check if threshold is met
            if (currentPercent >= requiredPercent && !slumberTriggered) {
                triggerSlumber(world, sleepingPlayers, totalPlayers, players);
                slumberTriggered = true;
            }
            
        } catch (Exception e) {
            logger.fine("[SleepService] Error checking world sleep: " + e.getMessage());
        }
    }
    
    private List<PlayerRef> getPlayersInWorld(World world) {
        List<PlayerRef> result = new ArrayList<>();
        try {
            Universe universe = Universe.get();
            if (universe == null) return result;
            
            java.util.UUID worldUuid = world.getWorldConfig().getUuid();
            
            for (PlayerRef player : universe.getPlayers()) {
                if (player != null && player.isValid()) {
                    java.util.UUID playerWorldUuid = player.getWorldUuid();
                    if (playerWorldUuid != null && playerWorldUuid.equals(worldUuid)) {
                        result.add(player);
                    }
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return result;
    }

    private boolean isPlayerInBed(PlayerRef player) {
        try {
            if (player == null || !player.isValid()) {
                return false;
            }
            
            Holder<EntityStore> holder = player.getHolder();
            if (holder == null) {
                return false;
            }
            
            // Check for PlayerSomnolence component
            PlayerSomnolence somnolence = holder.getComponent(PlayerSomnolence.getComponentType());
            if (somnolence == null) {
                return false;
            }
            
            // Check the sleep state - we want players who are NoddingOff or in Slumber
            PlayerSleep state = somnolence.getSleepState();
            if (state == null) {
                return false;
            }
            
            // Player is sleeping if they're in any sleep state (not fully awake)
            return !(state instanceof PlayerSleep.FullyAwake);
            
        } catch (Exception e) {
            return false;
        }
    }

    private void triggerSlumber(World world, int sleeping, int total, List<PlayerRef> players) {
        logger.fine("[SleepService] Triggering night skip in " + world.getName() + 
                   " (" + sleeping + "/" + total + " sleeping)");
        
        // Notify players
        for (PlayerRef player : players) {
            try {
                player.sendMessage(Message.join(
                    Message.raw("☾ ").color("#FFFF55"),
                    Message.raw(sleeping + "/" + total + " players sleeping").color("#AAAAAA"),
                    Message.raw(" - Skipping to morning!").color("#FFFF55")
                ));
            } catch (Exception e) {
                // Ignore message errors
            }
        }
        
        // Trigger the slumber on the world's thread
        world.execute(() -> {
            try {
                EntityStore entityStore = world.getEntityStore();
                Store<EntityStore> store = entityStore.getStore();
                
                // Get the world time resource to calculate wake-up time
                WorldTimeResource timeResource = store.getResource(WorldTimeResource.getResourceType());
                if (timeResource == null) {
                    logger.warning("[SleepService] Could not get WorldTimeResource");
                    return;
                }
                
                Instant currentTime = timeResource.getGameTime();
                
                // Calculate morning time (skip to 6:00 AM game time)
                // We need to find the next morning
                Instant morningTime = calculateNextMorning(currentTime, world);
                
                // Create and set the WorldSlumber state (3.0f = animation duration)
                WorldSlumber slumber = new WorldSlumber(currentTime, morningTime, 3.0f);
                
                // Get or create WorldSomnolence resource and set it to slumber
                WorldSomnolence worldSomnolence = store.getResource(WorldSomnolence.getResourceType());
                if (worldSomnolence != null) {
                    worldSomnolence.setState(slumber);
                    logger.fine("[SleepService] WorldSlumber state set successfully");
                } else {
                    logger.warning("[SleepService] WorldSomnolence resource not found");
                }
                
            } catch (Exception e) {
                logger.warning("[SleepService] Error triggering slumber: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    private Instant calculateNextMorning(Instant currentTime, World world) {
        try {
            // Get day/night duration from world config
            int daytimeSeconds = world.getDaytimeDurationSeconds();
            int nighttimeSeconds = world.getNighttimeDurationSeconds();
            int fullDaySeconds = daytimeSeconds + nighttimeSeconds;
            
            // Calculate time until morning (6:00 AM = start of daytime)
            // This is approximate - we skip forward by the remaining night time
            // For simplicity, skip forward 8 hours of game time
            return currentTime.plus(Duration.ofSeconds(nighttimeSeconds / 2));
        } catch (Exception e) {
            // Fallback: skip 8 hours
            return currentTime.plus(Duration.ofHours(8));
        }
    }

    public int getSleepPercentage() {
        return configManager.getConfig().sleep.sleepPercentage;
    }

    public void setSleepPercentage(int percent) {
        configManager.getConfig().sleep.sleepPercentage = Math.max(0, Math.min(100, percent));
        configManager.saveConfig();
        logger.fine("[SleepService] Sleep percentage set to " + percent + "%");
    }

    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
