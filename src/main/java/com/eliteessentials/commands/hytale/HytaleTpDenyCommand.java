package com.eliteessentials.commands.hytale;

import com.eliteessentials.model.TpaRequest;
import com.eliteessentials.services.TpaService;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.Optional;
import java.util.UUID;

/**
 * Command: /tpdeny
 * Denies a pending teleport request.
 */
public class HytaleTpDenyCommand extends AbstractPlayerCommand {

    private final TpaService tpaService;

    public HytaleTpDenyCommand(TpaService tpaService) {
        super("tpdeny", "Deny a teleport request");
        this.tpaService = tpaService;
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    @Override
    protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, 
                          PlayerRef player, World world) {
        UUID playerId = player.getUuid();
        
        Optional<TpaRequest> requestOpt = tpaService.denyRequest(playerId);

        if (requestOpt.isEmpty()) {
            ctx.sendMessage(Message.raw("You have no pending teleport requests.").color("#FF5555"));
            return;
        }

        TpaRequest request = requestOpt.get();
        
        ctx.sendMessage(Message.join(
            Message.raw("Teleport request from ").color("#FF5555"),
            Message.raw(request.getRequesterName()).color("#FFFFFF"),
            Message.raw(" denied.").color("#FF5555")
        ));
        
        // Notify the requester that their request was denied
        PlayerRef requester = Universe.get().getPlayer(request.getRequesterId());
        if (requester != null && requester.isValid()) {
            requester.sendMessage(Message.join(
                Message.raw(player.getUsername()).color("#FFFFFF"),
                Message.raw(" denied your teleport request.").color("#FF5555")
            ));
        }
    }
}
