package com.eliteessentials.commands.hytale;

import com.eliteessentials.config.ConfigManager;
import com.eliteessentials.config.PluginConfig;
import com.eliteessentials.permissions.Permissions;
import com.eliteessentials.permissions.PermissionService;
import com.eliteessentials.services.CooldownService;
import com.eliteessentials.util.CommandPermissionUtil;
import com.eliteessentials.util.MessageFormatter;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.UUID;
import javax.annotation.Nonnull;

/**
 * Command: /heal
 * Restores the player's health to full.
 * 
 * Permissions:
 * - eliteessentials.command.misc.heal - Use /heal command
 * - eliteessentials.command.misc.heal.bypass.cooldown - Skip cooldown
 * - eliteessentials.command.misc.heal.cooldown.<seconds> - Set specific cooldown (e.g., heal.cooldown.300 for 5 min)
 */
public class HytaleHealCommand extends AbstractPlayerCommand {

    private static final String COMMAND_NAME = "heal";
    
    private final ConfigManager configManager;
    private final CooldownService cooldownService;

    public HytaleHealCommand(ConfigManager configManager, CooldownService cooldownService) {
        super(COMMAND_NAME, "Restore your health to full");
        this.configManager = configManager;
        this.cooldownService = cooldownService;
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref,
                          @Nonnull PlayerRef player, @Nonnull World world) {
        PluginConfig.HealConfig healConfig = configManager.getConfig().heal;
        UUID playerId = player.getUuid();
        
        // Check permission (Admin command)
        if (!CommandPermissionUtil.canExecuteAdmin(ctx, player, Permissions.HEAL, healConfig.enabled)) {
            return;
        }
        
        // Get effective cooldown from permissions (handles bypass and per-group cooldowns)
        int effectiveCooldown = PermissionService.get().getHealCooldown(playerId);
        
        // Check cooldown if player has one
        if (effectiveCooldown > 0) {
            int cooldownRemaining = cooldownService.getCooldownRemaining(COMMAND_NAME, playerId);
            if (cooldownRemaining > 0) {
                ctx.sendMessage(MessageFormatter.formatWithFallback(
                    configManager.getMessage("onCooldown", "seconds", String.valueOf(cooldownRemaining)), "#FF5555"));
                return;
            }
        }

        // Get player's stat map
        EntityStatMap statMap = store.getComponent(ref, EntityStatMap.getComponentType());
        if (statMap == null) {
            ctx.sendMessage(MessageFormatter.formatWithFallback(configManager.getMessage("healFailed"), "#FF5555"));
            return;
        }

        // Maximize health
        int healthStatIndex = DefaultEntityStatTypes.getHealth();
        statMap.maximizeStatValue(healthStatIndex);
        
        // Set cooldown based on effective cooldown
        if (effectiveCooldown > 0) {
            cooldownService.setCooldown(COMMAND_NAME, playerId, effectiveCooldown);
        }

        ctx.sendMessage(MessageFormatter.formatWithFallback(configManager.getMessage("healSuccess"), "#55FF55"));
    }
}
