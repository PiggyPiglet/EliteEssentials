package com.eliteessentials.listeners;

import com.eliteessentials.model.Location;
import com.eliteessentials.services.DeathTrackingService;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * Listener for player death events.
 * Records death location for /back if enabled in config.
 * 
 * This class provides a hook point for when Hytale exposes death events.
 * Currently, death detection is handled by DeathTrackingService via polling.
 */
public class PlayerDeathListener {

    private static final Logger logger = Logger.getLogger("EliteEssentials");
    private final DeathTrackingService deathTrackingService;

    public PlayerDeathListener(DeathTrackingService deathTrackingService) {
        this.deathTrackingService = deathTrackingService;
    }

    /**
     * Called when a player dies.
     * Records their death location for /back (if enabled).
     * 
     * Hook this method into Hytale's death event system when available.
     * 
     * @param playerId Player UUID
     * @param deathLocation Location where they died
     */
    public void onPlayerDeath(UUID playerId, Location deathLocation) {
        deathTrackingService.recordDeath(playerId, deathLocation);
        logger.info("[PlayerDeathListener] Recorded death for " + playerId + " at " + deathLocation);
    }
    
    /**
     * Called when a player dies but we don't have their exact location.
     * Uses the last known position from tracking.
     * 
     * @param playerId Player UUID
     */
    public void onPlayerDeath(UUID playerId) {
        Location lastKnown = deathTrackingService.getLastKnownPosition(playerId);
        if (lastKnown != null) {
            deathTrackingService.recordDeath(playerId, lastKnown);
            logger.info("[PlayerDeathListener] Recorded death for " + playerId + " using last known position");
        } else {
            logger.warning("[PlayerDeathListener] Could not record death for " + playerId + " - no known position");
        }
    }
}
