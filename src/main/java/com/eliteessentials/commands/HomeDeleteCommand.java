package com.eliteessentials.commands;

import com.eliteessentials.services.HomeService;
import com.eliteessentials.util.MessageUtil;

import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Command: /delhome <name>
 * Deletes a saved home.
 * 
 * Usage:
 *   /delhome <name> - Delete a named home
 *   /homedelete <name> - Alias
 */
public class HomeDeleteCommand {

    private static final Logger logger = Logger.getLogger("EliteEssentials");
    private final HomeService homeService;

    public HomeDeleteCommand(HomeService homeService) {
        this.homeService = homeService;
    }

    /**
     * Execute the delhome command.
     */
    public CommandResult execute(UUID playerId, String playerName, String[] args) {
        if (args.length == 0) {
            return CommandResult.error("&cUsage: /delhome <name>");
        }

        String homeName = args[0];
        HomeService.Result result = homeService.deleteHome(playerId, homeName);

        return switch (result) {
            case SUCCESS -> {
                logger.info(playerName + " deleted home '" + homeName + "'");
                yield CommandResult.success(MessageUtil.getMessage("homeDeleted")
                        .replace("{name}", homeName));
            }
            case HOME_NOT_FOUND -> CommandResult.error(MessageUtil.getMessage("homeNotFound")
                    .replace("{name}", homeName));
            default -> CommandResult.error("&cFailed to delete home.");
        };
    }

    /**
     * Tab completion for home names.
     */
    public Set<String> tabComplete(UUID playerId, String partial) {
        Set<String> homes = homeService.getHomeNames(playerId);
        if (partial == null || partial.isEmpty()) {
            return homes;
        }
        
        String lowerPartial = partial.toLowerCase();
        return homes.stream()
                .filter(name -> name.toLowerCase().startsWith(lowerPartial))
                .collect(java.util.stream.Collectors.toSet());
    }
}
