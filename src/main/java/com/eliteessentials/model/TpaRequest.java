package com.eliteessentials.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a teleport request from one player to another.
 */
public class TpaRequest {

    public enum Type {
        TPA,      // Requester teleports to target
        TPAHERE   // Target teleports to requester
    }

    private final UUID requesterId;
    private final String requesterName;
    private final UUID targetId;
    private final String targetName;
    private final Type type;
    private final long createdAt;
    private final long expiresAt;

    public TpaRequest(UUID requesterId, String requesterName, UUID targetId, String targetName, int timeoutSeconds) {
        this(requesterId, requesterName, targetId, targetName, Type.TPA, timeoutSeconds);
    }

    public TpaRequest(UUID requesterId, String requesterName, UUID targetId, String targetName, Type type, int timeoutSeconds) {
        this.requesterId = requesterId;
        this.requesterName = requesterName;
        this.targetId = targetId;
        this.targetName = targetName;
        this.type = type;
        this.createdAt = Instant.now().toEpochMilli();
        this.expiresAt = createdAt + (timeoutSeconds * 1000L);
    }

    public UUID getRequesterId() {
        return requesterId;
    }

    public String getRequesterName() {
        return requesterName;
    }

    public UUID getTargetId() {
        return targetId;
    }

    public String getTargetName() {
        return targetName;
    }

    public Type getType() {
        return type;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public boolean isExpired() {
        return Instant.now().toEpochMilli() > expiresAt;
    }

    public int getRemainingSeconds() {
        long remaining = expiresAt - Instant.now().toEpochMilli();
        return (int) Math.max(0, remaining / 1000);
    }

    @Override
    public String toString() {
        return String.format("TpaRequest{type=%s, from='%s', to='%s', expires=%ds}",
                type, requesterName, targetName, getRemainingSeconds());
    }
}
