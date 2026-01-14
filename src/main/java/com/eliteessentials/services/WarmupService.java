package com.eliteessentials.services;

import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * Service for handling teleport warmups - players must stand still for a duration.
 */
public class WarmupService {

    private static final Logger logger = Logger.getLogger("EliteEssentials");
    private static final double MOVEMENT_THRESHOLD = 0.5; // Max movement allowed during warmup (0.5 blocks)

    private final ScheduledExecutorService scheduler;
    private final Map<UUID, WarmupTask> activeWarmups = new ConcurrentHashMap<>();

    public WarmupService() {
        this.scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "EliteEssentials-Warmup");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Start a warmup for a player. They must stand still for the duration.
     * 
     * @param player The player
     * @param startPosition The player's starting position
     * @param warmupSeconds Duration to stand still
     * @param onComplete Action to run when warmup completes successfully
     * @param commandName Name of the command for messages
     * @param world The world (for scheduling checks on game thread)
     * @param store The entity store
     * @param ref The entity reference
     */
    public void startWarmup(PlayerRef player, Vector3d startPosition, int warmupSeconds, 
                            Runnable onComplete, String commandName,
                            World world, Store<EntityStore> store, Ref<EntityStore> ref) {
        logger.info("[Warmup] startWarmup called for " + commandName + " with " + warmupSeconds + "s");
        
        UUID playerId = player.getUuid();
        
        // Cancel any existing warmup
        cancelWarmup(playerId);
        
        if (warmupSeconds <= 0) {
            logger.info("[Warmup] warmupSeconds <= 0, executing immediately");
            onComplete.run();
            return;
        }
        
        if (startPosition == null) {
            logger.warning("[Warmup] No start position provided, executing immediately");
            onComplete.run();
            return;
        }
        
        logger.info("[Warmup] Starting " + warmupSeconds + "s warmup for " + commandName);
        
        // Create warmup task
        WarmupTask task = new WarmupTask(player, startPosition, onComplete, commandName, 
                                          warmupSeconds, world, store, ref);
        activeWarmups.put(playerId, task);
        
        // Schedule movement checks every 500ms
        ScheduledFuture<?> checkFuture = scheduler.scheduleAtFixedRate(
            () -> {
                logger.fine("[Warmup] Scheduled movement check for " + playerId);
                checkMovementOnGameThread(playerId);
            },
            500, 500, TimeUnit.MILLISECONDS
        );
        task.setCheckFuture(checkFuture);
        logger.info("[Warmup] Movement checks scheduled every 500ms");
        
        // Schedule completion
        ScheduledFuture<?> completeFuture = scheduler.schedule(
            () -> completeWarmup(playerId),
            warmupSeconds, TimeUnit.SECONDS
        );
        task.setCompleteFuture(completeFuture);
        
        logger.info("[Warmup] Warmup scheduled with movement checking");
    }
    
    /**
     * Overload for commands that don't have world/store/ref - no movement checking.
     */
    public void startWarmup(PlayerRef player, Vector3d startPosition, int warmupSeconds, 
                            Runnable onComplete, String commandName) {
        startWarmup(player, startPosition, warmupSeconds, onComplete, commandName, null, null, null);
    }
    
    private void checkMovementOnGameThread(UUID playerId) {
        WarmupTask task = activeWarmups.get(playerId);
        if (task == null) {
            logger.fine("[Warmup] No task found for " + playerId + " during movement check");
            return;
        }
        
        World world = task.getWorld();
        Store<EntityStore> store = task.getStore();
        Ref<EntityStore> ref = task.getRef();
        
        if (world == null || store == null || ref == null) {
            logger.fine("[Warmup] Missing world/store/ref for movement check");
            return;
        }
        
        // Schedule the check on the game thread
        world.execute(() -> {
            try {
                // Check if task still exists (might have been cancelled)
                if (!activeWarmups.containsKey(playerId)) {
                    return;
                }
                
                TransformComponent transform = (TransformComponent) store.getComponent(ref, TransformComponent.getComponentType());
                if (transform == null) {
                    logger.fine("[Warmup] Could not get transform component");
                    return;
                }
                
                Vector3d currentPos = transform.getPosition();
                Vector3d startPos = task.getStartPosition();
                
                double dx = currentPos.getX() - startPos.getX();
                double dy = currentPos.getY() - startPos.getY();
                double dz = currentPos.getZ() - startPos.getZ();
                double distance = Math.sqrt(dx*dx + dy*dy + dz*dz);
                
                logger.fine("[Warmup] Movement check: distance = " + String.format("%.2f", distance) + " blocks");
                
                if (distance > MOVEMENT_THRESHOLD) {
                    // Player moved, cancel warmup
                    logger.info("[Warmup] Player " + playerId + " moved " + String.format("%.2f", distance) + 
                              " blocks (threshold: " + MOVEMENT_THRESHOLD + "), cancelling warmup");
                    
                    PlayerRef player = task.getPlayer();
                    if (player != null && player.isValid()) {
                        player.sendMessage(Message.raw("Teleport cancelled - you moved!").color("#FF5555"));
                    }
                    
                    cancelWarmup(playerId);
                }
            } catch (Exception e) {
                logger.warning("[Warmup] Error during movement check: " + e.getMessage());
            }
        });
    }

    private void completeWarmup(UUID playerId) {
        logger.info("[Warmup] Completing warmup for " + playerId);
        
        WarmupTask task = activeWarmups.remove(playerId);
        if (task == null) {
            logger.warning("[Warmup] No task found for " + playerId);
            return;
        }
        
        // Cancel the check task
        if (task.getCheckFuture() != null) {
            task.getCheckFuture().cancel(false);
        }
        
        // Execute the teleport
        try {
            logger.info("[Warmup] Executing teleport action for " + task.getCommandName());
            task.getOnComplete().run();
        } catch (Exception e) {
            logger.warning("[Warmup] Error completing warmup: " + e.getMessage());
        }
    }

    public void cancelWarmup(UUID playerId) {
        WarmupTask task = activeWarmups.remove(playerId);
        if (task != null) {
            if (task.getCheckFuture() != null) {
                task.getCheckFuture().cancel(false);
            }
            if (task.getCompleteFuture() != null) {
                task.getCompleteFuture().cancel(false);
            }
            logger.info("[Warmup] Cancelled warmup for " + playerId);
        }
    }

    public boolean hasActiveWarmup(UUID playerId) {
        return activeWarmups.containsKey(playerId);
    }

    public void shutdown() {
        for (UUID playerId : activeWarmups.keySet()) {
            cancelWarmup(playerId);
        }
        
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

    /**
     * Internal class to track warmup state.
     */
    private static class WarmupTask {
        private final PlayerRef player;
        private final Vector3d startPosition;
        private final Runnable onComplete;
        private final String commandName;
        private final int durationSeconds;
        private final World world;
        private final Store<EntityStore> store;
        private final Ref<EntityStore> ref;
        private ScheduledFuture<?> checkFuture;
        private ScheduledFuture<?> completeFuture;

        WarmupTask(PlayerRef player, Vector3d startPosition, Runnable onComplete, 
                   String commandName, int durationSeconds,
                   World world, Store<EntityStore> store, Ref<EntityStore> ref) {
            this.player = player;
            this.startPosition = startPosition;
            this.onComplete = onComplete;
            this.commandName = commandName;
            this.durationSeconds = durationSeconds;
            this.world = world;
            this.store = store;
            this.ref = ref;
        }

        PlayerRef getPlayer() { return player; }
        Vector3d getStartPosition() { return startPosition; }
        Runnable getOnComplete() { return onComplete; }
        String getCommandName() { return commandName; }
        int getDurationSeconds() { return durationSeconds; }
        World getWorld() { return world; }
        Store<EntityStore> getStore() { return store; }
        Ref<EntityStore> getRef() { return ref; }
        
        ScheduledFuture<?> getCheckFuture() { return checkFuture; }
        void setCheckFuture(ScheduledFuture<?> future) { this.checkFuture = future; }
        
        ScheduledFuture<?> getCompleteFuture() { return completeFuture; }
        void setCompleteFuture(ScheduledFuture<?> future) { this.completeFuture = future; }
    }
}
