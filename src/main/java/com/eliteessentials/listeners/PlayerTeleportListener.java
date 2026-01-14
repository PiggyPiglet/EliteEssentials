package com.eliteessentials.listeners;

import com.eliteessentials.model.Location;
import com.eliteessentials.services.BackService;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * Listener for player teleport events.
 * Records locations for the /back command.
 * 
 * TODO: Implement actual event handling once Hytale API is available.
 * This is a placeholder that shows the expected structure.
 */
public class PlayerTeleportListener {

    private static final Logger logger = Logger.getLogger("EliteEssentials");
    private final BackService backService;

    public PlayerTeleportListener(BackService backService) {
        this.backService = backService;
    }

    /**
     * Called when a player teleports.
     * Records their previous location for /back.
     * 
     * @param playerId Player UUID
     * @param fromLocation Location they teleported from
     * @param toLocation Location they teleported to
     * @param cause Cause of the teleport (command, plugin, etc.)
     */
    public void onPlayerTeleport(UUID playerId, Location fromLocation, Location toLocation, String cause) {
        // Only record location for certain teleport causes
        // Skip if it's a /back teleport to prevent loops
        if ("BACK_COMMAND".equals(cause)) {
            return;
        }

        // Record the location they left from
        backService.pushLocation(playerId, fromLocation);
        
        logger.fine("Recorded pre-teleport location for " + playerId + ": " + fromLocation);
    }
}
