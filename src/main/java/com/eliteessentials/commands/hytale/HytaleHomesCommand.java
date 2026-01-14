package com.eliteessentials.commands.hytale;

import com.eliteessentials.services.HomeService;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.Set;
import java.util.UUID;

/**
 * Command: /homes
 * Lists all player homes.
 */
public class HytaleHomesCommand extends AbstractPlayerCommand {

    private final HomeService homeService;

    public HytaleHomesCommand(HomeService homeService) {
        super("homes", "List your homes");
        this.homeService = homeService;
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    @Override
    protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, 
                          PlayerRef player, World world) {
        UUID playerId = player.getUuid();
        Set<String> homes = homeService.getHomeNames(playerId);
        
        if (homes.isEmpty()) {
            ctx.sendMessage(Message.raw("You have no homes set. Use /sethome to create one.").color("#FFAA00"));
            return;
        }

        int count = homes.size();
        int max = homeService.getMaxHomes(playerId);
        
        ctx.sendMessage(Message.join(
            Message.raw("Your homes (").color("#55FF55"),
            Message.raw(count + "/" + max).color("#FFFFFF"),
            Message.raw("): ").color("#55FF55"),
            Message.raw(String.join(", ", homes)).color("#FFFFFF")
        ));
    }
}
