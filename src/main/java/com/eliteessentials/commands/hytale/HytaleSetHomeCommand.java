package com.eliteessentials.commands.hytale;

import com.eliteessentials.model.Location;
import com.eliteessentials.services.HomeService;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.UUID;

/**
 * Command: /sethome [name]
 * Sets a home at the player's current location.
 * Base command (no args) uses "home" as default name.
 */
public class HytaleSetHomeCommand extends AbstractPlayerCommand {

    private final HomeService homeService;

    public HytaleSetHomeCommand(HomeService homeService) {
        super("sethome", "Set your home location");
        this.homeService = homeService;
        
        // Add variant that accepts a name argument
        addUsageVariant(new SetHomeWithNameCommand(homeService));
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    @Override
    protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, 
                          PlayerRef player, World world) {
        // Default: use "home" as name
        setHome(ctx, store, ref, player, world, "home");
    }
    
    static void setHome(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                        PlayerRef player, World world, String homeName, HomeService homeService) {
        TransformComponent transform = (TransformComponent) store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) {
            ctx.sendMessage(Message.raw("Could not get your position.").color("#FF5555"));
            return;
        }

        HeadRotation headRotation = (HeadRotation) store.getComponent(ref, HeadRotation.getComponentType());
        Vector3f rotation = headRotation != null ? headRotation.getRotation() : new Vector3f(0, 0, 0);
        
        Vector3d position = transform.getPosition();
        UUID playerId = player.getUuid();

        Location location = new Location(
            world.getName(),
            position.getX(),
            position.getY(),
            position.getZ(),
            rotation.getYaw(),
            rotation.getPitch()
        );

        HomeService.Result result = homeService.setHome(playerId, homeName, location);

        switch (result) {
            case SUCCESS -> ctx.sendMessage(Message.join(
                Message.raw("Home '").color("#55FF55"),
                Message.raw(homeName).color("#FFFFFF"),
                Message.raw("' set at ").color("#55FF55"),
                Message.raw(String.format("%.1f, %.1f, %.1f", position.getX(), position.getY(), position.getZ())).color("#FFFFFF"),
                Message.raw(" in ").color("#55FF55"),
                Message.raw(world.getName()).color("#FFFFFF")
            ));
            case LIMIT_REACHED -> {
                int max = homeService.getMaxHomes(playerId);
                ctx.sendMessage(Message.raw("You have reached your home limit (" + max + ").").color("#FF5555"));
            }
            case INVALID_NAME -> ctx.sendMessage(Message.raw("Invalid home name.").color("#FF5555"));
            default -> ctx.sendMessage(Message.raw("Failed to set home.").color("#FF5555"));
        }
    }
    
    private void setHome(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                         PlayerRef player, World world, String homeName) {
        setHome(ctx, store, ref, player, world, homeName, homeService);
    }
    
    /**
     * Variant: /sethome <name>
     */
    private static class SetHomeWithNameCommand extends AbstractPlayerCommand {
        private final HomeService homeService;
        private final RequiredArg<String> nameArg;
        
        SetHomeWithNameCommand(HomeService homeService) {
            super("sethome");
            this.homeService = homeService;
            this.nameArg = withRequiredArg("name", "Home name", ArgTypes.STRING);
        }
        
        @Override
        protected boolean canGeneratePermission() {
            return false;
        }
        
        @Override
        protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                              PlayerRef player, World world) {
            String homeName = ctx.get(nameArg);
            HytaleSetHomeCommand.setHome(ctx, store, ref, player, world, homeName, homeService);
        }
    }
}
