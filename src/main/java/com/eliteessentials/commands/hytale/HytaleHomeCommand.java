package com.eliteessentials.commands.hytale;

import com.eliteessentials.model.Home;
import com.eliteessentials.model.Location;
import com.eliteessentials.services.BackService;
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
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Command: /home [name]
 * Teleports the player to their saved home location.
 * Base command (no args) uses "home" as default name.
 */
public class HytaleHomeCommand extends AbstractPlayerCommand {

    private final HomeService homeService;
    private final BackService backService;

    public HytaleHomeCommand(HomeService homeService, BackService backService) {
        super("home", "Teleport to your home location");
        this.homeService = homeService;
        this.backService = backService;
        
        // Add variant that accepts a name argument
        addUsageVariant(new HomeWithNameCommand(homeService, backService));
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    @Override
    protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, 
                          PlayerRef player, World world) {
        // Default: use "home" as name
        goHome(ctx, store, ref, player, world, "home", homeService, backService);
    }
    
    static void goHome(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                       PlayerRef player, World world, String homeName,
                       HomeService homeService, BackService backService) {
        UUID playerId = player.getUuid();
        
        Optional<Home> homeOpt = homeService.getHome(playerId, homeName);
        
        if (homeOpt.isEmpty()) {
            Set<String> homes = homeService.getHomeNames(playerId);
            if (homes.isEmpty()) {
                ctx.sendMessage(Message.raw("You don't have a home set. Use /sethome first.").color("#FF5555"));
                return;
            }
            ctx.sendMessage(Message.join(
                Message.raw("Home '").color("#FF5555"),
                Message.raw(homeName).color("#FFFFFF"),
                Message.raw("' not found. Your homes: ").color("#FF5555"),
                Message.raw(String.join(", ", homes)).color("#FFFFFF")
            ));
            return;
        }

        Home home = homeOpt.get();
        Location loc = home.getLocation();

        // Save current location for /back
        TransformComponent transform = (TransformComponent) store.getComponent(ref, TransformComponent.getComponentType());
        if (transform != null) {
            HeadRotation headRotation = (HeadRotation) store.getComponent(ref, HeadRotation.getComponentType());
            Vector3f rotation = headRotation != null ? headRotation.getRotation() : new Vector3f(0, 0, 0);
            Vector3d position = transform.getPosition();
            
            Location currentLoc = new Location(
                world.getName(),
                position.getX(), position.getY(), position.getZ(),
                rotation.getYaw(), rotation.getPitch()
            );
            backService.pushLocation(playerId, currentLoc);
        }

        // Get target world
        World targetWorld = Universe.get().getWorld(loc.getWorld());
        if (targetWorld == null) {
            targetWorld = world; // Fallback to current world
        }

        // Teleport player
        final World finalWorld = targetWorld;
        final String finalHomeName = homeName;
        world.execute(() -> {
            Vector3d targetPos = new Vector3d(loc.getX(), loc.getY(), loc.getZ());
            Vector3f targetRot = new Vector3f(loc.getPitch(), loc.getYaw(), 0);
            
            Teleport teleport = new Teleport(finalWorld, targetPos, targetRot);
            store.addComponent(ref, Teleport.getComponentType(), teleport);
            
            ctx.sendMessage(Message.join(
                Message.raw("Teleporting to home '").color("#55FF55"),
                Message.raw(finalHomeName).color("#FFFFFF"),
                Message.raw("'").color("#55FF55")
            ));
        });
    }
    
    /**
     * Variant: /home <name>
     */
    private static class HomeWithNameCommand extends AbstractPlayerCommand {
        private final HomeService homeService;
        private final BackService backService;
        private final RequiredArg<String> nameArg;
        
        HomeWithNameCommand(HomeService homeService, BackService backService) {
            super("home");
            this.homeService = homeService;
            this.backService = backService;
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
            HytaleHomeCommand.goHome(ctx, store, ref, player, world, homeName, homeService, backService);
        }
    }
}
