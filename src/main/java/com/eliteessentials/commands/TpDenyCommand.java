package com.eliteessentials.commands;

import com.eliteessentials.model.TpaRequest;
import com.eliteessentials.services.TpaService;
import com.eliteessentials.util.MessageUtil;

import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Command: /tpdeny [player]
 * Denies a pending teleport request.
 * 
 * Usage:
 *   /tpdeny         - Deny most recent request
 *   /tpdeny <player> - Deny request from specific player
 */
public class TpDenyCommand {

    private static final Logger logger = Logger.getLogger("EliteEssentials");
    private final TpaService tpaService;

    public TpDenyCommand(TpaService tpaService) {
        this.tpaService = tpaService;
    }

    /**
     * Execute the tpdeny command.
     */
    public CommandResult execute(UUID playerId, String playerName, String[] args,
                                  java.util.function.Function<String, UUID> requesterIdLookup) {
        Optional<TpaRequest> requestOpt;
        
        if (args.length > 0) {
            UUID requesterId = requesterIdLookup.apply(args[0]);
            if (requesterId == null) {
                return CommandResult.error(MessageUtil.getMessage("playerNotFound"));
            }
            requestOpt = tpaService.denyRequestFrom(playerId, requesterId);
        } else {
            requestOpt = tpaService.denyRequest(playerId);
        }

        if (requestOpt.isEmpty()) {
            return CommandResult.error(MessageUtil.getMessage("tpaNoPending"));
        }

        TpaRequest request = requestOpt.get();
        
        logger.info("TPA denied: " + request.getRequesterName() + " -> " + playerName);
        
        return CommandResult.tpaDenied(
                request.getRequesterId(),
                MessageUtil.getMessage("tpaDenied")
        );
    }
}
