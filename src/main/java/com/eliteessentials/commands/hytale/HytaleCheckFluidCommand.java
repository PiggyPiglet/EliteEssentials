package com.eliteessentials.commands.hytale;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.util.MathUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.lang.reflect.Method;

/**
 * TEMPORARY DEBUG COMMAND: /checkfluid
 * Checks fluid ID at your current location.
 * Use this to discover lava's fluid ID.
 * 
 * DELETE THIS FILE AFTER TESTING!
 */
public class HytaleCheckFluidCommand extends AbstractPlayerCommand {

    public HytaleCheckFluidCommand() {
        super("checkfluid", "Check fluid at current location (DEBUG)");
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    @Override
    protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                          PlayerRef player, World world) {
        
        // Get player position
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) {
            ctx.sendMessage(Message.raw("Could not get position").color("#FF5555"));
            return;
        }

        Vector3d pos = transform.getPosition();
        int blockX = MathUtil.floor(pos.x);
        int blockY = MathUtil.floor(pos.y);
        int blockZ = MathUtil.floor(pos.z);

        // Get chunk
        long chunkIndex = ChunkUtil.indexChunkFromBlock(blockX, blockZ);
        WorldChunk chunk = world.getChunk(chunkIndex);
        if (chunk == null) {
            ctx.sendMessage(Message.raw("Chunk not loaded").color("#FF5555"));
            return;
        }

        ctx.sendMessage(Message.raw("=== FLUID CHECK ===").color("#FFFF55"));
        ctx.sendMessage(Message.raw("Position: " + blockX + ", " + blockY + ", " + blockZ).color("#AAAAAA"));

        try {
            Method getFluidIdMethod = chunk.getClass().getMethod("getFluidId", int.class, int.class, int.class);
            Method getFluidLevelMethod = chunk.getClass().getMethod("getFluidLevel", int.class, int.class, int.class);

            // Check current position and surrounding blocks
            for (int yOffset = -2; yOffset <= 2; yOffset++) {
                int checkY = blockY + yOffset;
                if (checkY < 0 || checkY >= 256) continue;

                Object fluidIdObj = getFluidIdMethod.invoke(chunk, blockX, checkY, blockZ);
                Object fluidLevelObj = getFluidLevelMethod.invoke(chunk, blockX, checkY, blockZ);

                int fluidId = (fluidIdObj instanceof Integer) ? (Integer) fluidIdObj : -1;
                byte fluidLevel = (fluidLevelObj instanceof Byte) ? (Byte) fluidLevelObj : -1;

                String yLabel = "Y" + (yOffset >= 0 ? "+" : "") + yOffset + " (" + checkY + ")";
                
                if (fluidId != 0) {
                    String fluidType = "UNKNOWN";
                    if (fluidId == 6) fluidType = "LAVA";
                    if (fluidId == 7) fluidType = "WATER";
                    
                    ctx.sendMessage(Message.raw(yLabel + ": FluidId=" + fluidId + " (" + fluidType + ") Level=" + fluidLevel).color("#FF5555"));
                } else {
                    ctx.sendMessage(Message.raw(yLabel + ": No fluid").color("#55FF55"));
                }
            }

            // Check horizontally
            ctx.sendMessage(Message.raw("--- Adjacent Blocks ---").color("#FFFF55"));
            int[][] offsets = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            String[] directions = {"East", "West", "South", "North"};

            for (int i = 0; i < offsets.length; i++) {
                int checkX = blockX + offsets[i][0];
                int checkZ = blockZ + offsets[i][1];

                Object fluidIdObj = getFluidIdMethod.invoke(chunk, checkX, blockY, checkZ);
                int fluidId = (fluidIdObj instanceof Integer) ? (Integer) fluidIdObj : -1;

                if (fluidId != 0) {
                    String fluidType = "UNKNOWN";
                    if (fluidId == 6) fluidType = "LAVA";
                    if (fluidId == 7) fluidType = "WATER";
                    ctx.sendMessage(Message.raw(directions[i] + ": FluidId=" + fluidId + " (" + fluidType + ")").color("#FF5555"));
                }
            }

            ctx.sendMessage(Message.raw("==================").color("#FFFF55"));

        } catch (Exception e) {
            ctx.sendMessage(Message.raw("Error: " + e.getMessage()).color("#FF5555"));
        }
    }
}
