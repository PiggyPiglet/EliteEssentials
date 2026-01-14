package com.eliteessentials.commands.hytale;

import com.eliteessentials.EliteEssentials;
import com.eliteessentials.config.PluginConfig;
import com.eliteessentials.model.Location;
import com.eliteessentials.services.BackService;
import com.eliteessentials.services.CooldownService;
import com.eliteessentials.services.WarmupService;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.WorldConfig;
import com.hypixel.hytale.server.core.universe.world.spawn.ISpawnProvider;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.UUID;

/**
 * Command: /spawn
 * Teleports the player to the world spawn point.
 * Supports warmup (stand still) and cooldown.
 */
public class HytaleSpawnCommand extends AbstractPlayerCommand {

    private static final String COMMAND_NAME = "spawn";
    private final BackService backService;

    public HytaleSpawnCommand(BackService backService) {
        super("spawn", "Teleport to spawn");
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
        PluginConfig config = EliteEssentials.getInstance().getConfigManager().getConfig();
        CooldownService cooldownService = EliteEssentials.getInstance().getCooldownService();
        WarmupService warmupService = EliteEssentials.getInstance().getWarmupService();
        
        // Check cooldown
        int cooldownRemaining = cooldownService.getCooldownRemaining(COMMAND_NAME, playerId);
        if (cooldownRemaining > 0) {
            ctx.sendMessage(Message.raw("You must wait " + cooldownRemaining + " seconds before using /spawn again.").color("#FF5555"));
            return;
        }
        
        // Get spawn point from world config
        WorldConfig worldConfig = world.getWorldConfig();
        ISpawnProvider spawnProvider = worldConfig.getSpawnProvider();
        
        if (spawnProvider == null) {
            ctx.sendMessage(Message.raw("No spawn point configured for this world.").color("#FF5555"));
            return;
        }
        
        Transform spawnTransform = spawnProvider.getSpawnPoint(world, playerId);
        
        if (spawnTransform == null) {
            ctx.sendMessage(Message.raw("Could not find spawn point.").color("#FF5555"));
            return;
        }
        
        // Get warmup config
        int warmupSeconds = config.spawn.warmupSeconds;
        
        // Get current position for /back and warmup
        TransformComponent currentTransform = (TransformComponent) store.getComponent(ref, TransformComponent.getComponentType());
        if (currentTransform == null) {
            ctx.sendMessage(Message.raw("Could not get your position.").color("#FF5555"));
            return;
        }
        
        Vector3d currentPos = currentTransform.getPosition();
        HeadRotation headRotation = (HeadRotation) store.getComponent(ref, HeadRotation.getComponentType());
        Vector3f currentRot = headRotation != null ? headRotation.getRotation() : new Vector3f(0, 0, 0);
        
        Location currentLoc = new Location(
            world.getName(),
            currentPos.getX(), currentPos.getY(), currentPos.getZ(),
            currentRot.x, currentRot.y
        );
        
        // Define the actual teleport action
        Runnable doTeleport = () -> {
            // Save location for /back
            backService.pushLocation(playerId, currentLoc);

            // Teleport player to spawn
            Vector3d spawnPos = spawnTransform.getPosition();
            Vector3f spawnRot = spawnTransform.getRotation();
            
            world.execute(() -> {
                Teleport teleport = new Teleport(world, spawnPos, spawnRot);
                store.addComponent(ref, Teleport.getComponentType(), teleport);
                
                ctx.sendMessage(Message.raw("Teleported to spawn!").color("#55FF55"));
            });
            
            // Set cooldown
            cooldownService.setCooldown(COMMAND_NAME, playerId, config.spawn.cooldownSeconds);
        };
        
        // Start warmup or teleport immediately
        if (warmupSeconds > 0) {
            ctx.sendMessage(Message.join(
                Message.raw("Teleporting to spawn in ").color("#FFAA00"),
                Message.raw(warmupSeconds + " seconds").color("#FFFFFF"),
                Message.raw("... Stand still!").color("#FFAA00")
            ));
        }
        warmupService.startWarmup(player, currentPos, warmupSeconds, doTeleport, COMMAND_NAME, world, store, ref);
    }
}
