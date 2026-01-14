package com.eliteessentials.commands;

import com.eliteessentials.model.Location;
import com.eliteessentials.services.BackService;
import com.eliteessentials.util.MessageUtil;

import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Command: /back
 * Teleports the player to their previous location.
 * 
 * Location history is tracked on:
 * - Teleport commands (/home, /tpa, /rtp, etc.)
 * - Death (if enabled in config)
 */
public class BackCommand {

    private static final Logger logger = Logger.getLogger("EliteEssentials");
    private final BackService backService;

    public BackCommand(BackService backService) {
        this.backService = backService;
    }

    /**
     * Execute the back command.
     * 
     * @param playerId Player UUID
     * @param playerName Player name
     * @param currentLocation Player's current location (to push before teleport)
     * @return Response to send to player
     */
    public CommandResult execute(UUID playerId, String playerName, Location currentLocation) {
        Optional<Location> previousLocation = backService.popLocation(playerId);
        
        if (previousLocation.isEmpty()) {
            return CommandResult.error(MessageUtil.getMessage("noBackLocation"));
        }

        Location destination = previousLocation.get();
        
        // Push current location so they can /back again to return
        backService.pushLocation(playerId, currentLocation);
        
        logger.info(playerName + " teleporting back to " + destination);
        
        return CommandResult.teleport(
                destination,
                MessageUtil.getMessage("teleportedBack")
        );
    }

    /**
     * Get the number of stored back locations for a player.
     */
    public int getHistorySize(UUID playerId) {
        return backService.getHistorySize(playerId);
    }
}
