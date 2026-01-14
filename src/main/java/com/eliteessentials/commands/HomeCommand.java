package com.eliteessentials.commands;

import com.eliteessentials.EliteEssentials;
import com.eliteessentials.model.Home;
import com.eliteessentials.services.HomeService;
import com.eliteessentials.util.MessageUtil;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Command: /home [name]
 * Teleports the player to their saved home location.
 * 
 * Usage:
 *   /home        - Teleport to default home (if only one exists, or "home")
 *   /home <name> - Teleport to named home
 *   /homes       - List all homes (alias)
 */
public class HomeCommand {

    private static final Logger logger = Logger.getLogger("EliteEssentials");
    private final HomeService homeService;

    public HomeCommand(HomeService homeService) {
        this.homeService = homeService;
    }

    /**
     * Execute the home command.
     * 
     * @param playerId Player UUID
     * @param playerName Player name (for logging)
     * @param args Command arguments
     * @return Response message to send to player
     */
    public CommandResult execute(UUID playerId, String playerName, String[] args) {
        // Determine home name
        String homeName = args.length > 0 ? args[0] : "home";

        // Get the home
        Optional<Home> homeOpt = homeService.getHome(playerId, homeName);
        
        if (homeOpt.isEmpty()) {
            // If no args and no "home", list available homes
            if (args.length == 0) {
                Set<String> homes = homeService.getHomeNames(playerId);
                if (homes.isEmpty()) {
                    return CommandResult.error(MessageUtil.getMessage("homeNotFound")
                            .replace("{name}", homeName));
                }
                if (homes.size() == 1) {
                    // Teleport to the only home they have
                    homeName = homes.iterator().next();
                    homeOpt = homeService.getHome(playerId, homeName);
                } else {
                    return CommandResult.error("&cYou have multiple homes. Use &f/home <name>&c. Available: &f" 
                            + String.join(", ", homes));
                }
            } else {
                return CommandResult.error(MessageUtil.getMessage("homeNotFound")
                        .replace("{name}", homeName));
            }
        }

        Home home = homeOpt.get();
        
        logger.info(playerName + " teleporting to home '" + home.getName() + "'");
        
        return CommandResult.teleport(
                home.getLocation(),
                MessageUtil.getMessage("teleportedHome").replace("{name}", home.getName())
        );
    }

    /**
     * List all homes for a player.
     */
    public CommandResult listHomes(UUID playerId) {
        Set<String> homes = homeService.getHomeNames(playerId);
        
        if (homes.isEmpty()) {
            return CommandResult.message("&7You have no homes set. Use &f/homeset <name>&7 to create one.");
        }

        int count = homes.size();
        int max = homeService.getMaxHomes(playerId);
        
        StringBuilder sb = new StringBuilder();
        sb.append("&7Your homes (&f").append(count).append("&7/&f").append(max).append("&7): ");
        sb.append("&f").append(String.join("&7, &f", homes));
        
        return CommandResult.message(sb.toString());
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
