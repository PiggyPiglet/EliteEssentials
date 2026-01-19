package com.eliteessentials.commands.hytale;

import com.eliteessentials.EliteEssentials;
import com.eliteessentials.config.ConfigManager;
import com.eliteessentials.permissions.Permissions;
import com.eliteessentials.services.TpaService;
import com.eliteessentials.util.CommandPermissionUtil;
import com.eliteessentials.util.MessageFormatter;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Command: /tpa <player>
 * Sends a teleport request to another player.
 * 
 * Permissions:
 * - eliteessentials.command.tpa - Send teleport requests
 */
public class HytaleTpaCommand extends AbstractPlayerCommand {

    private static final String COMMAND_NAME = "tpa";
    
    private final TpaService tpaService;
    private final RequiredArg<PlayerRef> targetArg;

    public HytaleTpaCommand(TpaService tpaService) {
        super(COMMAND_NAME, "Request to teleport to a player");
        this.tpaService = tpaService;
        this.targetArg = withRequiredArg("player", "Target player", ArgTypes.PLAYER_REF);
        
        // Permission check handled in execute() via CommandPermissionUtil
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    @Override
    protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, 
                          PlayerRef player, World world) {
        boolean enabled = EliteEssentials.getInstance().getConfigManager().getConfig().tpa.enabled;
        if (!CommandPermissionUtil.canExecute(ctx, player, Permissions.TPA, enabled)) {
            return;
        }
        
        ConfigManager configManager = EliteEssentials.getInstance().getConfigManager();
        PlayerRef target = ctx.get(targetArg);
        
        if (target == null) {
            ctx.sendMessage(MessageFormatter.formatWithFallback(configManager.getMessage("playerNotFound"), "#FF5555"));
            return;
        }
        
        // Create teleport request
        TpaService.Result result = tpaService.createRequest(
            player.getUuid(),
            player.getUsername(),
            target.getUuid(),
            target.getUsername()
        );
        
        switch (result) {
            case REQUEST_SENT -> {
                ctx.sendMessage(MessageFormatter.formatWithFallback(configManager.getMessage("tpaRequestSent", "player", target.getUsername()), "#55FF55"));
                
                // Send notification to target player with instructions
                target.sendMessage(MessageFormatter.formatWithFallback(configManager.getMessage("tpaRequestReceived", "player", player.getUsername()), "#FFFF55"));
                target.sendMessage(MessageFormatter.formatWithFallback(configManager.getMessage("tpaRequestInstructions"), "#AAAAAA"));
            }
            case SELF_REQUEST -> ctx.sendMessage(MessageFormatter.formatWithFallback(configManager.getMessage("tpaSelfRequest"), "#FF5555"));
            case ALREADY_PENDING -> ctx.sendMessage(MessageFormatter.formatWithFallback(configManager.getMessage("tpaAlreadyPending"), "#FF5555"));
            default -> ctx.sendMessage(MessageFormatter.formatWithFallback(configManager.getMessage("tpaRequestFailed"), "#FF5555"));
        }
    }
}
