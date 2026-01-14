package com.eliteessentials.systems;

import com.eliteessentials.model.Location;
import com.eliteessentials.services.BackService;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefChangeSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * ECS System that listens for DeathComponent being added to entities.
 * When a player dies, this captures their death location for /back.
 * 
 * This mirrors how Hytale's DeathSystems$PlayerDeathMarker works internally.
 */
public class PlayerDeathSystem extends RefChangeSystem<EntityStore, DeathComponent> {

    private static final Logger logger = Logger.getLogger("EliteEssentials");
    
    private final BackService backService;

    public PlayerDeathSystem(BackService backService) {
        this.backService = backService;
        logger.info("[PlayerDeathSystem] Death tracking system created");
    }

    @Override
    public ComponentType<EntityStore, DeathComponent> componentType() {
        return DeathComponent.getComponentType();
    }

    @Override
    public Query<EntityStore> getQuery() {
        // Accept any entity with DeathComponent - we filter for players in onComponentAdded
        return Query.any();
    }

    @Override
    public void onComponentAdded(Ref<EntityStore> ref, DeathComponent deathComponent,
                                  Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer) {
        try {
            // Get Universe - might be null during early startup
            Universe universe = Universe.get();
            if (universe == null) {
                logger.fine("[PlayerDeathSystem] Universe not available yet");
                return;
            }
            
            // Get the player's UUID - check if this is a player entity
            ComponentType<EntityStore, PlayerRef> playerRefType = universe.getPlayerRefComponentType();
            if (playerRefType == null) {
                logger.fine("[PlayerDeathSystem] PlayerRef component type not available");
                return;
            }
            
            PlayerRef playerRef = store.getComponent(ref, playerRefType);
            if (playerRef == null) {
                // Not a player entity, ignore (could be NPC or mob)
                return;
            }
            
            UUID playerId = playerRef.getUuid();
            logger.fine("[PlayerDeathSystem] DEATH DETECTED for player " + playerId);
            
            // Get the player's position at death
            TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
            if (transform == null) {
                logger.warning("[PlayerDeathSystem] Could not get transform for dead player " + playerId);
                return;
            }
            
            Vector3d pos = transform.getPosition();
            
            // Get rotation if available
            HeadRotation headRotation = store.getComponent(ref, HeadRotation.getComponentType());
            float yaw = 0f, pitch = 0f;
            if (headRotation != null) {
                Vector3f rotation = headRotation.getRotation();
                yaw = rotation.getYaw();
                pitch = rotation.getPitch();
            }
            
            // Get world name from the store's external data (EntityStore -> World)
            String worldName = "world";
            try {
                EntityStore entityStore = store.getExternalData();
                if (entityStore != null && entityStore.getWorld() != null) {
                    worldName = entityStore.getWorld().getName();
                }
            } catch (Exception e) {
                // Use default world name
            }
            
            Location deathLocation = new Location(worldName, pos.getX(), pos.getY(), pos.getZ(), yaw, pitch);
            
            logger.fine("[PlayerDeathSystem] Player " + playerId + " DIED at " + 
                String.format("%.1f, %.1f, %.1f in %s", pos.getX(), pos.getY(), pos.getZ(), worldName));
            
            // Save to back service (which persists to JSON)
            backService.pushDeathLocation(playerId, deathLocation);
            
        } catch (Exception e) {
            logger.warning("[PlayerDeathSystem] Error processing death: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onComponentSet(Ref<EntityStore> ref, DeathComponent oldComponent, DeathComponent newComponent,
                                Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer) {
        // Death component being updated - we only care about initial add
    }

    @Override
    public void onComponentRemoved(Ref<EntityStore> ref, DeathComponent deathComponent,
                                    Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer) {
        // Death component removed (respawn) - nothing to do
    }
}
