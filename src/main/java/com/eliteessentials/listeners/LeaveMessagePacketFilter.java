package com.eliteessentials.listeners;

import com.hypixel.hytale.protocol.FormattedMessage;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.interface_.ServerMessage;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PacketFilter;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketFilter;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.lang.reflect.Field;
import java.util.logging.Logger;

/**
 * Packet filter to suppress default Hytale leave messages.
 * This prevents the "player has left world" message from being sent to clients,
 * allowing us to send our own custom colored messages instead.
 * 
 * This approach is cleaner than translation overrides because:
 * - No blank lines in chat
 * - More targeted (filters specific packets)
 * - Prevents the message from being sent at all
 */
public final class LeaveMessagePacketFilter implements PlayerPacketFilter {
    
    private static final Logger logger = Logger.getLogger("EliteEssentials");
    private static final Field MESSAGE_FIELD;
    private static final Field MESSAGE_ID_FIELD;
    
    // Translation key for default Hytale leave message
    private static final String LEAVE_MESSAGE_ID = "server.general.playerLeftWorld";
    
    // Store the registered filter so we can deregister it if needed
    private static PacketFilter registeredFilter = null;
    
    static {
        MESSAGE_FIELD = findField(ServerMessage.class, "message");
        MESSAGE_ID_FIELD = findField(FormattedMessage.class, "messageId");
    }
    
    /**
     * Find and make accessible a field in a class using reflection.
     */
    private static Field findField(Class<?> clazz, String name) {
        try {
            Field field = clazz.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (Exception e) {
            logger.severe("Failed to access field '" + name + "' in " + clazz.getSimpleName() + ": " + e.getMessage());
            throw new RuntimeException("Failed to access field: " + name, e);
        }
    }
    
    /**
     * Register this filter globally for all players.
     * This should be called once during plugin initialization.
     */
    public static void register() {
        if (registeredFilter == null) {
            registeredFilter = PacketAdapters.registerOutbound(new LeaveMessagePacketFilter());
            logger.info("Registered leave message packet filter");
        }
    }
    
    /**
     * Deregister this filter.
     * This should be called during plugin shutdown or reload if needed.
     */
    public static void deregister() {
        if (registeredFilter != null) {
            PacketAdapters.deregisterOutbound(registeredFilter);
            registeredFilter = null;
            logger.info("Deregistered leave message packet filter");
        }
    }
    
    /**
     * Test if a packet should be filtered (blocked from being sent).
     * Returns true to block the packet, false to allow it through.
     */
    @Override
    public boolean test(PlayerRef playerRef, Packet packet) {
        // Only filter ServerMessage packets
        if (!(packet instanceof ServerMessage)) {
            return false;
        }
        
        try {
            // Extract the FormattedMessage from the ServerMessage packet
            FormattedMessage msg = (FormattedMessage) MESSAGE_FIELD.get(packet);
            if (msg == null) {
                return false;
            }
            
            // Check if this is the leave message we want to suppress
            String messageId = (String) MESSAGE_ID_FIELD.get(msg);
            return LEAVE_MESSAGE_ID.equals(messageId);
            
        } catch (IllegalAccessException e) {
            // If reflection fails, don't block the packet
            logger.warning("Failed to check packet message ID: " + e.getMessage());
            return false;
        }
    }
}
