package com.eliteessentials.commands;

import com.eliteessentials.model.TpaRequest;
import com.eliteessentials.services.BackService;
import com.eliteessentials.services.TpaService;
import com.eliteessentials.util.MessageUtil;

import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Command: /tpaccept [player]
 * Accepts a pending teleport request.
 * 
 * Usage:
 *   /tpaccept         - Accept most recent request
 *   /tpaccept <player> - Accept request from specific player
 */
public class TpAcceptCommand {

    private static final Logger logger = Logger.getLogger("EliteEssentials");
    private final TpaService tpaService;
    private final BackService backService;

    public TpAcceptCommand(TpaService tpaService, BackService backService) {
        this.tpaService = tpaService;
        this.backService = backService;
    }

    /**
     * Execute the tpaccept command.
     * 
     * @param playerId UUID of player accepting (target of the request)
     * @param playerName Name of accepting player
     * @param args Command arguments (optional: specific requester name)
     * @param requesterIdLookup Function to look up requester UUID by name (if specified)
     * @return Response with teleport info
     */
    public CommandResult execute(UUID playerId, String playerName, String[] args,
                                  java.util.function.Function<String, UUID> requesterIdLookup) {
        Optional<TpaRequest> requestOpt;
        
        if (args.length > 0) {
            // Accept from specific player
            UUID requesterId = requesterIdLookup.apply(args[0]);
            if (requesterId == null) {
                return CommandResult.error(MessageUtil.getMessage("playerNotFound"));
            }
            requestOpt = tpaService.acceptRequestFrom(playerId, requesterId);
        } else {
            // Accept most recent request
            requestOpt = tpaService.acceptRequest(playerId);
        }

        if (requestOpt.isEmpty()) {
            return CommandResult.error(MessageUtil.getMessage("tpaNoPending"));
        }

        TpaRequest request = requestOpt.get();
        
        if (request.isExpired()) {
            return CommandResult.error(MessageUtil.getMessage("tpaExpired"));
        }

        logger.info("TPA accepted: " + request.getRequesterName() + " -> " + playerName);
        
        // Return info for teleporting the requester to the accepter
        return CommandResult.tpaAccepted(
                request.getRequesterId(),
                request.getRequesterName(),
                playerId,
                MessageUtil.getMessage("tpaAccepted")
        );
    }
    
    /**
     * Called after the teleport to track back location.
     */
    public void onTeleportComplete(UUID requesterId, com.eliteessentials.model.Location fromLocation) {
        backService.pushLocation(requesterId, fromLocation);
    }
}
