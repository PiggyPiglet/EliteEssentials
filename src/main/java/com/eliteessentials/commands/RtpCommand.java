package com.eliteessentials.commands;

import com.eliteessentials.model.Location;
import com.eliteessentials.services.BackService;
import com.eliteessentials.services.RtpService;
import com.eliteessentials.util.MessageUtil;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * Command: /rtp
 * Teleports the player to a random location within the configured range.
 * 
 * Features:
 * - Configurable min/max range
 * - Cooldown between uses
 * - Safe location finding (solid ground, no lava, etc.)
 */
public class RtpCommand {

    private static final Logger logger = Logger.getLogger("EliteEssentials");
    private final RtpService rtpService;
    private final BackService backService;

    public RtpCommand(RtpService rtpService, BackService backService) {
        this.rtpService = rtpService;
        this.backService = backService;
    }

    /**
     * Execute the RTP command.
     * 
     * Note: The actual safe location finding and teleportation will need to use
     * Hytale's world API. This method handles the logic flow.
     * 
     * @param playerId Player UUID
     * @param playerName Player name
     * @param currentLocation Player's current location
     * @param worldCenterX World center X for RTP range (e.g., spawn)
     * @param worldCenterZ World center Z for RTP range
     * @return Response to send to player
     */
    public CommandResult execute(UUID playerId, String playerName, Location currentLocation,
                                  double worldCenterX, double worldCenterZ) {
        // Check cooldown
        int cooldownRemaining = rtpService.getCooldownRemaining(playerId);
        if (cooldownRemaining > 0) {
            return CommandResult.error(MessageUtil.getMessage("rtpCooldown")
                    .replace("{seconds}", String.valueOf(cooldownRemaining)));
        }

        // Generate random locations to try
        String world = currentLocation.getWorld();
        Location[] candidates = rtpService.generateRandomLocations(
                worldCenterX, worldCenterZ, world, rtpService.getMaxAttempts()
        );

        // Return the candidates for the caller to find a safe location
        // The actual implementation will need to:
        // 1. Send "searching" message
        // 2. Check each candidate for safe ground
        // 3. Teleport to first safe location
        // 4. Set cooldown on success
        
        logger.info(playerName + " using /rtp, generated " + candidates.length + " candidates");
        
        return CommandResult.rtpCandidates(
                candidates,
                MessageUtil.getMessage("rtpSearching"),
                MessageUtil.getMessage("rtpSuccess"),
                MessageUtil.getMessage("rtpFailed")
        );
    }

    /**
     * Called after successful RTP to set cooldown and track location.
     */
    public void onRtpSuccess(UUID playerId, Location fromLocation) {
        rtpService.setCooldown(playerId);
        backService.pushLocation(playerId, fromLocation);
    }

    /**
     * Get remaining cooldown for display.
     */
    public int getCooldownRemaining(UUID playerId) {
        return rtpService.getCooldownRemaining(playerId);
    }
}
