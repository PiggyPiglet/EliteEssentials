package com.eliteessentials.gui;

import com.eliteessentials.EliteEssentials;
import com.eliteessentials.config.ConfigManager;
import com.eliteessentials.config.PluginConfig;
import com.eliteessentials.model.Location;
import com.eliteessentials.model.Warp;
import com.eliteessentials.permissions.PermissionService;
import com.eliteessentials.permissions.Permissions;
import com.eliteessentials.services.BackService;
import com.eliteessentials.services.CooldownService;
import com.eliteessentials.services.CostService;
import com.eliteessentials.services.WarpService;
import com.eliteessentials.services.WarmupService;
import com.eliteessentials.util.CommandPermissionUtil;
import com.eliteessentials.util.MessageFormatter;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * EliteEssentials Warp Selection GUI.
 * Shows NAME - Description with optional delete button for admins.
 */
public class WarpSelectionPage extends InteractiveCustomUIPage<WarpSelectionPage.WarpPageData> {

    private static final Logger logger = Logger.getLogger("EliteEssentials");
    private static final String COMMAND_NAME = "warp";
    
    private final WarpService warpService;
    private final BackService backService;
    private final ConfigManager configManager;
    private final World world;
    private final boolean canDelete;

    public WarpSelectionPage(PlayerRef playerRef, WarpService warpService, BackService backService, 
                             ConfigManager configManager, World world,
                             Ref<EntityStore> ref, Store<EntityStore> store) {
        super(playerRef, CustomPageLifetime.CanDismiss, WarpPageData.CODEC);
        this.warpService = warpService;
        this.backService = backService;
        this.configManager = configManager;
        this.world = world;
        
        // Check if player can delete warps
        PermissionService perms = PermissionService.get();
        this.canDelete = perms.canUseAdminCommand(playerRef.getUuid(), Permissions.DELWARP, true);
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder commandBuilder,
                      UIEventBuilder eventBuilder, Store<EntityStore> store) {
        commandBuilder.append("Pages/EliteEssentials_WarpPage.ui");
        
        String title = configManager.getMessage("guiWarpsTitle");
        commandBuilder.set("#TitleLabel.Text", title);

        buildWarpList(commandBuilder, eventBuilder);
    }
    
    private void buildWarpList(UICommandBuilder commandBuilder, UIEventBuilder eventBuilder) {
        UUID playerId = playerRef.getUuid();
        PermissionService perms = PermissionService.get();
        
        List<Warp> accessibleWarps = new ArrayList<>();
        for (Warp warp : warpService.getAllWarps().values()) {
            if (perms.canAccessWarp(playerId, warp.getName(), warp.getPermission())) {
                accessibleWarps.add(warp);
            }
        }
        accessibleWarps.sort(Comparator.comparing(Warp::getName));

        if (accessibleWarps.isEmpty()) {
            return;
        }

        for (int i = 0; i < accessibleWarps.size(); i++) {
            Warp warp = accessibleWarps.get(i);
            String selector = "#WarpCards[" + i + "]";

            // Use different UI file based on delete permission
            if (canDelete) {
                commandBuilder.append("#WarpCards", "Pages/EliteEssentials_WarpEntry.ui");
                eventBuilder.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    selector + " #DeleteButton",
                    EventData.of("Warp", "D:" + warp.getName())
                );
            } else {
                commandBuilder.append("#WarpCards", "Pages/EliteEssentials_WarpEntryNoDelete.ui");
            }
            
            commandBuilder.set(selector + " #WarpName.Text", warp.getName());
            
            // Show description
            String description = warp.getDescription() != null ? warp.getDescription() : "";
            commandBuilder.set(selector + " #WarpDescription.Text", description);

            // Bind teleport button
            eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                selector + " #TeleportButton",
                EventData.of("Warp", "T:" + warp.getName())
            );
        }
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, WarpPageData data) {
        if (data.warp == null || data.warp.isEmpty()) {
            return;
        }

        UUID playerId = playerRef.getUuid();
        PluginConfig config = configManager.getConfig();
        PermissionService perms = PermissionService.get();
        
        // Check if delete action
        if (data.warp.startsWith("D:")) {
            String warpName = data.warp.substring(2);
            handleDeleteWarp(playerId, warpName);
            return;
        }
        
        String warpName = data.warp.startsWith("T:") ? data.warp.substring(2) : data.warp;
        
        Optional<Warp> warpOpt = warpService.getWarp(warpName);
        
        if (warpOpt.isEmpty()) {
            sendMessage(configManager.getMessage("warpNotFound", "name", warpName), "#FF5555");
            this.close();
            return;
        }

        Warp warp = warpOpt.get();

        if (!perms.canAccessWarp(playerId, warp.getName(), warp.getPermission())) {
            sendMessage(configManager.getMessage("warpNoPermission"), "#FF5555");
            this.close();
            return;
        }

        this.close();
        
        CooldownService cooldownService = EliteEssentials.getInstance().getCooldownService();
        
        if (!CommandPermissionUtil.canBypassCooldown(playerId, COMMAND_NAME)) {
            int cooldownRemaining = cooldownService.getCooldownRemaining(COMMAND_NAME, playerId);
            if (cooldownRemaining > 0) {
                sendMessage(configManager.getMessage("onCooldown", "seconds", String.valueOf(cooldownRemaining)), "#FF5555");
                return;
            }
        }
        
        CostService costService = EliteEssentials.getInstance().getCostService();
        double cost = config.warps.cost;
        if (costService != null && cost > 0) {
            if (!costService.canAfford(playerId, cost)) {
                sendMessage(configManager.getMessage("notEnoughMoney", "cost", costService.formatCost(cost)), "#FF5555");
                return;
            }
        }
        
        WarmupService warmupService = EliteEssentials.getInstance().getWarmupService();
        
        if (warmupService.hasActiveWarmup(playerId)) {
            sendMessage(configManager.getMessage("teleportInProgress"), "#FF5555");
            return;
        }
        
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) {
            sendMessage(configManager.getMessage("couldNotGetPosition"), "#FF5555");
            return;
        }
        
        Vector3d currentPos = transform.getPosition();
        HeadRotation headRotation = store.getComponent(ref, HeadRotation.getComponentType());
        Vector3f rotation = headRotation != null ? headRotation.getRotation() : new Vector3f(0, 0, 0);
        
        com.eliteessentials.model.Location currentLoc = new com.eliteessentials.model.Location(
            world.getName(),
            currentPos.getX(), currentPos.getY(), currentPos.getZ(),
            rotation.y, 0f
        );
        
        Location loc = warp.getLocation();
        World targetWorld = Universe.get().getWorld(loc.getWorld());
        if (targetWorld == null) {
            targetWorld = world;
        }
        final World finalWorld = targetWorld;
        final String finalWarpName = warp.getName();
        final CostService finalCostService = costService;
        final double finalCost = cost;
        final int configCooldown = config.warps.cooldownSeconds;
        
        Runnable doTeleport = () -> {
            backService.pushLocation(playerId, currentLoc);
            
            world.execute(() -> {
                Vector3d targetPos = new Vector3d(loc.getX(), loc.getY(), loc.getZ());
                Vector3f targetRot = new Vector3f(0, loc.getYaw(), 0);
                
                Teleport teleport = new Teleport(finalWorld, targetPos, targetRot);
                store.putComponent(ref, Teleport.getComponentType(), teleport);
                
                if (finalCostService != null && finalCost > 0) {
                    EliteEssentials.getInstance().getPlayerService().removeMoney(playerId, finalCost);
                }
                
                cooldownService.setCooldown(COMMAND_NAME, playerId, configCooldown);
                
                sendMessage(configManager.getMessage("warpTeleported", "name", finalWarpName), "#55FF55");
            });
        };

        int warmupSeconds = CommandPermissionUtil.getEffectiveWarmup(playerId, COMMAND_NAME, config.warps.warmupSeconds);
        
        if (warmupSeconds > 0) {
            sendMessage(configManager.getMessage("warpWarmup", "name", finalWarpName, "seconds", String.valueOf(warmupSeconds)), "#FFAA00");
        }
        
        warmupService.startWarmup(playerRef, currentPos, warmupSeconds, doTeleport, COMMAND_NAME, world, store, ref, false);
    }
    
    private void handleDeleteWarp(UUID playerId, String warpName) {
        PermissionService perms = PermissionService.get();
        if (!perms.canUseAdminCommand(playerId, Permissions.DELWARP, true)) {
            sendMessage(configManager.getMessage("noPermission"), "#FF5555");
            return;
        }
        
        Optional<Warp> warpOpt = warpService.getWarp(warpName);
        if (warpOpt.isEmpty()) {
            sendMessage(configManager.getMessage("warpNotFound", "name", warpName), "#FF5555");
            return;
        }
        
        boolean deleted = warpService.deleteWarp(warpName);
        
        if (deleted) {
            sendMessage(configManager.getMessage("warpDeleted", "name", warpName), "#55FF55");
            refreshWarpList();
        } else {
            sendMessage(configManager.getMessage("warpDeleteFailed"), "#FF5555");
        }
    }
    
    private void refreshWarpList() {
        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder events = new UIEventBuilder();
        
        cmd.set("#TitleLabel.Text", configManager.getMessage("guiWarpsTitle"));
        cmd.clear("#WarpCards");
        
        UUID playerId = playerRef.getUuid();
        PermissionService perms = PermissionService.get();
        
        List<Warp> accessibleWarps = new ArrayList<>();
        for (Warp warp : warpService.getAllWarps().values()) {
            if (perms.canAccessWarp(playerId, warp.getName(), warp.getPermission())) {
                accessibleWarps.add(warp);
            }
        }
        accessibleWarps.sort(Comparator.comparing(Warp::getName));
        
        for (int i = 0; i < accessibleWarps.size(); i++) {
            Warp warp = accessibleWarps.get(i);
            String selector = "#WarpCards[" + i + "]";

            // Use different UI file based on delete permission
            if (canDelete) {
                cmd.append("#WarpCards", "Pages/EliteEssentials_WarpEntry.ui");
                events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    selector + " #DeleteButton",
                    EventData.of("Warp", "D:" + warp.getName())
                );
            } else {
                cmd.append("#WarpCards", "Pages/EliteEssentials_WarpEntryNoDelete.ui");
            }
            
            cmd.set(selector + " #WarpName.Text", warp.getName());
            
            String description = warp.getDescription() != null ? warp.getDescription() : "";
            cmd.set(selector + " #WarpDescription.Text", description);

            events.addEventBinding(
                CustomUIEventBindingType.Activating,
                selector + " #TeleportButton",
                EventData.of("Warp", "T:" + warp.getName())
            );
        }
        
        this.sendUpdate(cmd, events, false);
    }

    private void sendMessage(String message, String color) {
        playerRef.sendMessage(MessageFormatter.formatWithFallback(message, color));
    }

    public static class WarpPageData {
        public static final BuilderCodec<WarpPageData> CODEC = BuilderCodec.builder(WarpPageData.class, WarpPageData::new)
                .append(new KeyedCodec<>("Warp", Codec.STRING), (data, s) -> data.warp = s, data -> data.warp)
                .add()
                .build();

        private String warp;

        public String getWarp() {
            return warp;
        }
    }
}
