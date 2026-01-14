package com.eliteessentials.commands;

import com.eliteessentials.model.Location;

import java.util.UUID;

/**
 * Represents the result of a command execution.
 * Contains all information needed for the command handler to perform actions.
 */
public class CommandResult {

    public enum Type {
        MESSAGE,           // Just send a message
        ERROR,             // Send error message
        TELEPORT,          // Teleport the sender
        RTP_CANDIDATES,    // RTP location candidates to check
        TPA_REQUEST_SENT,  // TPA request created, notify target
        TPA_ACCEPTED,      // TPA accepted, teleport requester
        TPA_DENIED         // TPA denied, notify requester
    }

    private final Type type;
    private final String message;
    private final String secondaryMessage;
    private final Location location;
    private final Location[] rtpCandidates;
    private final UUID targetPlayerId;
    private final String targetPlayerName;
    private final String rtpSuccessMessage;
    private final String rtpFailMessage;

    private CommandResult(Type type, String message, String secondaryMessage,
                          Location location, Location[] rtpCandidates,
                          UUID targetPlayerId, String targetPlayerName,
                          String rtpSuccessMessage, String rtpFailMessage) {
        this.type = type;
        this.message = message;
        this.secondaryMessage = secondaryMessage;
        this.location = location;
        this.rtpCandidates = rtpCandidates;
        this.targetPlayerId = targetPlayerId;
        this.targetPlayerName = targetPlayerName;
        this.rtpSuccessMessage = rtpSuccessMessage;
        this.rtpFailMessage = rtpFailMessage;
    }

    // Factory methods

    public static CommandResult message(String message) {
        return new CommandResult(Type.MESSAGE, message, null, null, null, null, null, null, null);
    }

    public static CommandResult success(String message) {
        return new CommandResult(Type.MESSAGE, message, null, null, null, null, null, null, null);
    }

    public static CommandResult error(String message) {
        return new CommandResult(Type.ERROR, message, null, null, null, null, null, null, null);
    }

    public static CommandResult teleport(Location location, String message) {
        return new CommandResult(Type.TELEPORT, message, null, location, null, null, null, null, null);
    }

    public static CommandResult rtpCandidates(Location[] candidates, String searchingMessage,
                                               String successMessage, String failMessage) {
        return new CommandResult(Type.RTP_CANDIDATES, searchingMessage, null, null, candidates,
                null, null, successMessage, failMessage);
    }

    public static CommandResult tpaRequestSent(UUID targetId, String senderMessage, String targetMessage) {
        return new CommandResult(Type.TPA_REQUEST_SENT, senderMessage, targetMessage, null, null,
                targetId, null, null, null);
    }

    public static CommandResult tpaAccepted(UUID requesterId, String requesterName, 
                                             UUID targetId, String message) {
        return new CommandResult(Type.TPA_ACCEPTED, message, null, null, null,
                targetId, requesterName, null, null);
    }

    public static CommandResult tpaDenied(UUID requesterId, String message) {
        return new CommandResult(Type.TPA_DENIED, message, null, null, null,
                requesterId, null, null, null);
    }

    // Getters

    public Type getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public String getSecondaryMessage() {
        return secondaryMessage;
    }

    public Location getLocation() {
        return location;
    }

    public Location[] getRtpCandidates() {
        return rtpCandidates;
    }

    public UUID getTargetPlayerId() {
        return targetPlayerId;
    }

    public String getTargetPlayerName() {
        return targetPlayerName;
    }

    public String getRtpSuccessMessage() {
        return rtpSuccessMessage;
    }

    public String getRtpFailMessage() {
        return rtpFailMessage;
    }

    public boolean isSuccess() {
        return type != Type.ERROR;
    }
}
