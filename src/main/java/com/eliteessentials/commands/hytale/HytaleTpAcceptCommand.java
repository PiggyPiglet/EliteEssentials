package com.eliteessentials.commands.hytale;

import com.eliteessentials.model.Location;
import com.eliteessentials.model.TpaRequest;
import com.eliteessentials.services.BackService;
import com.eliteessentials.services.TpaService;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Command: /tpaccept
 * Accepts a pending teleport request and teleports the requester to you.
 */
public class HytaleTpAcceptCommand extends AbstractPlayerCommand {

    private static final Logger logger = Logger.getLogger("EliteEssentials");
    
    private final TpaService tpaService;
    private final BackService backService;

    public HytaleTpAcceptCommand(TpaService tpaService, BackService backService) {
        super("tpaccept", "Accept a teleport request");
        this.tpaService = tpaService;
        this.backService = backService;
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    @Override
    protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, 
                          PlayerRef player, World world) {
        UUID playerId = player.getUuid();
        
        Optional<TpaRequest> requestOpt = tpaService.acceptRequest(playerId);

        if (requestOpt.isEmpty()) {
            ctx.sendMessage(Message.raw("You have no pending teleport requests.").color("#FF5555"));
            return;
        }

        TpaRequest request = requestOpt.get();
        
        if (request.isExpired()) {
            ctx.sendMessage(Message.raw("Teleport request has expired.").color("#FF5555"));
            return;
        }

        // Get the requester player from the server
        PlayerRef requester = Universe.get().getPlayer(request.getRequesterId());
        
        if (requester == null || !requester.isValid()) {
            ctx.sendMessage(Message.join(
                Message.raw(request.getRequesterName()).color("#FFFFFF"),
                Message.raw(" is no longer online.").color("#FF5555")
            ));
            return;
        }
        
        // Get target (acceptor's) position
        TransformComponent targetTransform = (TransformComponent) store.getComponent(ref, TransformComponent.getComponentType());
        if (targetTransform == null) {
            ctx.sendMessage(Message.raw("Could not determine your position.").color("#FF5555"));
            return;
        }
        
        Vector3d targetPos = targetTransform.getPosition();
        
        logger.fine("[TPA] Teleporting " + request.getRequesterName() + " to " + player.getUsername() + 
                   " at " + String.format("%.1f, %.1f, %.1f", targetPos.getX(), targetPos.getY(), targetPos.getZ()));
        
        // Get requester's current location for /back
        Holder<EntityStore> requesterHolder = requester.getHolder();
        
        if (requesterHolder != null) {
            TransformComponent requesterTransform = requesterHolder.getComponent(TransformComponent.getComponentType());
            if (requesterTransform != null) {
                Vector3d reqPos = requesterTransform.getPosition();
                HeadRotation reqHeadRot = requesterHolder.getComponent(HeadRotation.getComponentType());
                Vector3f reqRot = reqHeadRot != null ? reqHeadRot.getRotation() : new Vector3f(0, 0, 0);
                
                // Get requester's current world name
                World requesterWorld = Universe.get().getWorld(requester.getWorldUuid());
                String worldName = requesterWorld != null ? requesterWorld.getName() : world.getName();
                
                Location requesterLoc = new Location(
                    worldName,
                    reqPos.getX(), reqPos.getY(), reqPos.getZ(),
                    reqRot.x, reqRot.y
                );
                backService.pushLocation(request.getRequesterId(), requesterLoc);
            }
        }
        
        // Get the requester's world and store to properly teleport them
        World requesterWorld = Universe.get().getWorld(requester.getWorldUuid());
        if (requesterWorld == null) {
            requesterWorld = world; // Fallback to acceptor's world
        }
        
        final World finalRequesterWorld = requesterWorld;
        
        // Teleport the requester to the acceptor
        // We need to execute on the requester's world, then teleport to target world
        finalRequesterWorld.execute(() -> {
            try {
                // Get the requester's Ref in their current world's store
                EntityStore requesterEntityStore = finalRequesterWorld.getEntityStore();
                Store<EntityStore> requesterStore = requesterEntityStore.getStore();
                Ref<EntityStore> requesterRef = requesterEntityStore.getRefFromUUID(request.getRequesterId());
                
                if (requesterRef == null) {
                    logger.warning("[TPA] Could not find requester ref for " + request.getRequesterId());
                    return;
                }
                
                // Teleport directly to the acceptor's position (no offset to avoid wall clipping)
                Teleport teleport = new Teleport(world, targetPos, Vector3f.NaN);
                requesterStore.addComponent(requesterRef, Teleport.getComponentType(), teleport);
                
                logger.fine("[TPA] Teleport component added for " + request.getRequesterName());
            } catch (Exception e) {
                logger.warning("[TPA] Error teleporting: " + e.getMessage());
                e.printStackTrace();
            }
        });
        
        ctx.sendMessage(Message.join(
            Message.raw("Teleport request accepted! ").color("#55FF55"),
            Message.raw(request.getRequesterName()).color("#FFFFFF"),
            Message.raw(" is teleporting to you.").color("#55FF55")
        ));
        requester.sendMessage(Message.join(
            Message.raw(player.getUsername()).color("#FFFFFF"),
            Message.raw(" accepted your teleport request!").color("#55FF55")
        ));
    }
}
