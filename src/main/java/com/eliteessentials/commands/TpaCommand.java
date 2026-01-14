package com.eliteessentials.commands;

import com.eliteessentials.services.TpaService;
import com.eliteessentials.util.MessageUtil;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * Command: /tpa <player>
 * Sends a teleport request to another player.
 * 
 * The target player can accept with /tpaccept or deny with /tpdeny.
 * Request expires after the configured timeout (default 30s).
 */
public class TpaCommand {

    private static final Logger logger = Logger.getLogger("EliteEssentials");
    private final TpaService tpaService;

    public TpaCommand(TpaService tpaService) {
        this.tpaService = tpaService;
    }

    /**
     * Execute the tpa command.
     * 
     * @param requesterId UUID of player sending request
     * @param requesterName Name of requester
     * @param targetId UUID of target player (null if not found)
     * @param targetName Name of target player
     * @return Response to send to requester
     */
    public CommandResult execute(UUID requesterId, String requesterName, 
                                  UUID targetId, String targetName) {
        if (targetId == null) {
            return CommandResult.error(MessageUtil.getMessage("playerNotFound"));
        }

        TpaService.Result result = tpaService.createRequest(
                requesterId, requesterName, targetId, targetName
        );

        return switch (result) {
            case REQUEST_SENT -> {
                logger.info(requesterName + " sent tpa request to " + targetName);
                yield CommandResult.tpaRequestSent(
                        targetId,
                        MessageUtil.getMessage("tpaRequestSent").replace("{player}", targetName),
                        MessageUtil.getMessage("tpaRequestReceived").replace("{player}", requesterName)
                );
            }
            case ALREADY_PENDING -> CommandResult.error(MessageUtil.getMessage("tpaAlreadyPending"));
            case SELF_REQUEST -> CommandResult.error("&cYou cannot teleport to yourself.");
            default -> CommandResult.error("&cFailed to send teleport request.");
        };
    }
}
