package com.eliteessentials.commands.hytale;

import com.eliteessentials.EliteEssentials;
import com.eliteessentials.config.ConfigManager;
import com.eliteessentials.model.TpaRequest;
import com.eliteessentials.permissions.Permissions;
import com.eliteessentials.services.TpaService;
import com.eliteessentials.util.CommandPermissionUtil;
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
 * Command: /tpahere <player>
 * Requests a player to teleport to you (opposite of /tpa).
 * 
 * Permissions:
 * - eliteessentials.command.tpahere - Send tpahere requests
 */
public class HytaleTpahereCommand extends AbstractPlayerCommand {

    private static final String COMMAND_NAME = "tpahere";
    
    private final TpaService tpaService;
    private final RequiredArg<PlayerRef> targetArg;

    public HytaleTpahereCommand(TpaService tpaService) {
        super(COMMAND_NAME, "Request a player to teleport to you");
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
        if (!CommandPermissionUtil.canExecute(ctx, player, Permissions.TPAHERE, enabled)) {
            return;
        }
        
        ConfigManager configManager = EliteEssentials.getInstance().getConfigManager();
        PlayerRef target = ctx.get(targetArg);
        
        if (target == null) {
            ctx.sendMessage(Message.raw(configManager.getMessage("playerNotFound")).color("#FF5555"));
            return;
        }
        
        // Create teleport request with TPAHERE type
        TpaService.Result result = tpaService.createRequest(
            player.getUuid(),
            player.getUsername(),
            target.getUuid(),
            target.getUsername(),
            TpaRequest.Type.TPAHERE
        );
        
        switch (result) {
            case REQUEST_SENT -> {
                ctx.sendMessage(Message.raw(configManager.getMessage("tpahereRequestSent", "player", target.getUsername())).color("#55FF55"));
                
                // Send notification to target player with instructions
                target.sendMessage(Message.raw(configManager.getMessage("tpahereRequestReceived", "player", player.getUsername())).color("#FFFF55"));
                target.sendMessage(Message.raw(configManager.getMessage("tpaRequestInstructions")).color("#AAAAAA"));
            }
            case SELF_REQUEST -> ctx.sendMessage(Message.raw(configManager.getMessage("tpaSelfRequest")).color("#FF5555"));
            case ALREADY_PENDING -> ctx.sendMessage(Message.raw(configManager.getMessage("tpaAlreadyPending")).color("#FF5555"));
            default -> ctx.sendMessage(Message.raw(configManager.getMessage("tpaRequestFailed")).color("#FF5555"));
        }
    }
}
