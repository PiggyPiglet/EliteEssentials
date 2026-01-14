package com.eliteessentials.commands;

import com.eliteessentials.model.Location;
import com.eliteessentials.services.HomeService;
import com.eliteessentials.util.MessageUtil;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * Command: /homeset <name>
 * Sets a home at the player's current location.
 * 
 * Usage:
 *   /homeset <name> - Set a named home
 *   /sethome <name> - Alias
 */
public class HomeSetCommand {

    private static final Logger logger = Logger.getLogger("EliteEssentials");
    private final HomeService homeService;

    public HomeSetCommand(HomeService homeService) {
        this.homeService = homeService;
    }

    /**
     * Execute the homeset command.
     * 
     * @param playerId Player UUID
     * @param playerName Player name
     * @param location Player's current location
     * @param args Command arguments
     * @return Response to send to player
     */
    public CommandResult execute(UUID playerId, String playerName, Location location, String[] args) {
        if (args.length == 0) {
            return CommandResult.error("&cUsage: /homeset <name>");
        }

        String homeName = args[0];
        
        // Validate name
        if (homeName.length() > 32) {
            return CommandResult.error("&cHome name must be 32 characters or less.");
        }
        
        if (!homeName.matches("^[a-zA-Z0-9_-]+$")) {
            return CommandResult.error("&cHome name can only contain letters, numbers, underscores, and hyphens.");
        }

        HomeService.Result result = homeService.setHome(playerId, homeName, location);

        return switch (result) {
            case SUCCESS -> {
                logger.info(playerName + " set home '" + homeName + "' at " + location);
                yield CommandResult.success(MessageUtil.getMessage("homeSet")
                        .replace("{name}", homeName));
            }
            case LIMIT_REACHED -> {
                int max = homeService.getMaxHomes(playerId);
                yield CommandResult.error(MessageUtil.getMessage("homeLimitReached")
                        .replace("{max}", String.valueOf(max)));
            }
            case INVALID_NAME -> CommandResult.error("&cInvalid home name.");
            default -> CommandResult.error("&cFailed to set home.");
        };
    }
}
