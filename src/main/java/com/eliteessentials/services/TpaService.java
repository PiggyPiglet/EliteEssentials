package com.eliteessentials.services;

import com.eliteessentials.config.ConfigManager;
import com.eliteessentials.model.TpaRequest;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * Service for managing teleport requests between players.
 * Handles request creation, expiration, acceptance, and denial.
 */
public class TpaService {

    private static final Logger logger = Logger.getLogger("EliteEssentials");

    private final ConfigManager configManager;
    
    // Target UUID -> List of pending requests to that player
    private final Map<UUID, List<TpaRequest>> pendingRequests = new ConcurrentHashMap<>();
    
    // Scheduler for cleaning up expired requests
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "EliteEssentials-TpaCleanup");
        t.setDaemon(true);
        return t;
    });

    public TpaService(ConfigManager configManager) {
        this.configManager = configManager;
        
        // Schedule cleanup every 5 seconds
        scheduler.scheduleAtFixedRate(this::cleanupExpired, 5, 5, TimeUnit.SECONDS);
    }

    /**
     * Result of a TPA operation.
     */
    public enum Result {
        SUCCESS,
        REQUEST_SENT,
        REQUEST_ACCEPTED,
        REQUEST_DENIED,
        REQUEST_EXPIRED,
        NO_PENDING_REQUEST,
        ALREADY_PENDING,
        SELF_REQUEST,
        PLAYER_OFFLINE
    }

    /**
     * Create a new teleport request.
     * 
     * @param requesterId UUID of the player requesting to teleport
     * @param requesterName Name of the requester
     * @param targetId UUID of the target player
     * @param targetName Name of the target
     * @return Result of the operation
     */
    public Result createRequest(UUID requesterId, String requesterName, UUID targetId, String targetName) {
        if (requesterId.equals(targetId)) {
            return Result.SELF_REQUEST;
        }

        // Check for existing request from same player
        List<TpaRequest> targetRequests = pendingRequests.computeIfAbsent(targetId, k -> new CopyOnWriteArrayList<>());
        
        boolean alreadyPending = targetRequests.stream()
                .anyMatch(r -> r.getRequesterId().equals(requesterId) && !r.isExpired());
        
        if (alreadyPending) {
            return Result.ALREADY_PENDING;
        }

        TpaRequest request = new TpaRequest(
                requesterId,
                requesterName,
                targetId,
                targetName,
                configManager.getTpaTimeout()
        );
        
        targetRequests.add(request);
        logger.info("TPA request created: " + requesterName + " -> " + targetName);
        
        return Result.REQUEST_SENT;
    }

    /**
     * Accept the most recent teleport request for a player.
     * 
     * @param targetId UUID of the player accepting
     * @return The accepted request, or empty if none pending
     */
    public Optional<TpaRequest> acceptRequest(UUID targetId) {
        List<TpaRequest> requests = pendingRequests.get(targetId);
        if (requests == null || requests.isEmpty()) {
            return Optional.empty();
        }

        // Get most recent non-expired request
        TpaRequest request = null;
        for (int i = requests.size() - 1; i >= 0; i--) {
            TpaRequest r = requests.get(i);
            if (!r.isExpired()) {
                request = r;
                requests.remove(i);
                break;
            }
        }

        if (request == null) {
            return Optional.empty();
        }

        logger.info("TPA request accepted: " + request.getRequesterName() + " -> " + request.getTargetName());
        return Optional.of(request);
    }

    /**
     * Accept a specific request from a player.
     */
    public Optional<TpaRequest> acceptRequestFrom(UUID targetId, UUID requesterId) {
        List<TpaRequest> requests = pendingRequests.get(targetId);
        if (requests == null) return Optional.empty();

        for (int i = 0; i < requests.size(); i++) {
            TpaRequest r = requests.get(i);
            if (r.getRequesterId().equals(requesterId) && !r.isExpired()) {
                requests.remove(i);
                logger.info("TPA request accepted: " + r.getRequesterName() + " -> " + r.getTargetName());
                return Optional.of(r);
            }
        }
        return Optional.empty();
    }

    /**
     * Deny the most recent teleport request for a player.
     */
    public Optional<TpaRequest> denyRequest(UUID targetId) {
        List<TpaRequest> requests = pendingRequests.get(targetId);
        if (requests == null || requests.isEmpty()) {
            return Optional.empty();
        }

        // Remove most recent non-expired request
        for (int i = requests.size() - 1; i >= 0; i--) {
            TpaRequest r = requests.get(i);
            if (!r.isExpired()) {
                requests.remove(i);
                logger.info("TPA request denied: " + r.getRequesterName() + " -> " + r.getTargetName());
                return Optional.of(r);
            }
        }
        return Optional.empty();
    }

    /**
     * Deny a specific request from a player.
     */
    public Optional<TpaRequest> denyRequestFrom(UUID targetId, UUID requesterId) {
        List<TpaRequest> requests = pendingRequests.get(targetId);
        if (requests == null) return Optional.empty();

        for (int i = 0; i < requests.size(); i++) {
            TpaRequest r = requests.get(i);
            if (r.getRequesterId().equals(requesterId)) {
                requests.remove(i);
                logger.info("TPA request denied: " + r.getRequesterName() + " -> " + r.getTargetName());
                return Optional.of(r);
            }
        }
        return Optional.empty();
    }

    /**
     * Get all pending requests for a player.
     */
    public List<TpaRequest> getPendingRequests(UUID targetId) {
        List<TpaRequest> requests = pendingRequests.get(targetId);
        if (requests == null) return Collections.emptyList();
        
        // Return only non-expired
        return requests.stream()
                .filter(r -> !r.isExpired())
                .toList();
    }

    /**
     * Check if a player has any pending requests.
     */
    public boolean hasPendingRequests(UUID targetId) {
        List<TpaRequest> requests = pendingRequests.get(targetId);
        if (requests == null) return false;
        return requests.stream().anyMatch(r -> !r.isExpired());
    }

    /**
     * Cancel all outgoing requests from a player.
     */
    public void cancelOutgoingRequests(UUID requesterId) {
        for (List<TpaRequest> requests : pendingRequests.values()) {
            requests.removeIf(r -> r.getRequesterId().equals(requesterId));
        }
    }

    /**
     * Clean up expired requests.
     */
    private void cleanupExpired() {
        for (List<TpaRequest> requests : pendingRequests.values()) {
            requests.removeIf(TpaRequest::isExpired);
        }
    }

    /**
     * Shutdown the service and cleanup scheduler.
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        pendingRequests.clear();
    }
}
