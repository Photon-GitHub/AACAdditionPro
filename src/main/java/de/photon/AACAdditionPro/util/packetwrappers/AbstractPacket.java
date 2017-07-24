package de.photon.AACAdditionPro.util.packetwrappers;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.google.common.base.Objects;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;

public abstract class AbstractPacket
{
    // The packet we will be modifying
    protected PacketContainer handle;

    /**
     * Constructs a new strongly typed wrapper for the given packet.
     *
     * @param handle - handle to the raw packet data.
     * @param type   - the packet type.
     */
    protected AbstractPacket(final PacketContainer handle, final PacketType type)
    {
        // Make sure we're given a valid packet
        if (handle == null) {
            throw new IllegalArgumentException("Packet handle cannot be NULL.");
        }
        if (!Objects.equal(handle.getType(), type)) {
            throw new IllegalArgumentException(handle.getHandle()
                                               + " is not a packet of type " + type);
        }

        this.handle = handle;
    }

    /**
     * Retrieve a handle to the raw packet data.
     *
     * @return Raw packet data.
     */
    public PacketContainer getHandle()
    {
        return handle;
    }

    /**
     * Send the current packet to the given receiver.
     *
     * @param receiver - the receiver.
     *
     * @throws RuntimeException If the packet cannot be sent.
     */
    public void sendPacket(final Player receiver)
    {
        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(receiver,
                                                                  getHandle());
        } catch (final InvocationTargetException e) {
            throw new RuntimeException("Cannot send packet.", e);
        }
    }

    /**
     * Simulate receiving the current packet from the given sender.
     *
     * @param sender - the sender.
     *
     * @throws RuntimeException If the packet cannot be received.
     * @see #receivePacket(Player)
     * @deprecated Misspelled. recieve -> receive
     */
    @Deprecated
    public void recievePacket(final Player sender)
    {
        try {
            ProtocolLibrary.getProtocolManager().recieveClientPacket(sender,
                                                                     getHandle());
        } catch (final Exception e) {
            throw new RuntimeException("Cannot recieve packet.", e);
        }
    }

    /**
     * Simulate receiving the current packet from the given sender.
     *
     * @param sender - the sender.
     *
     * @throws RuntimeException if the packet cannot be received.
     */
    public void receivePacket(final Player sender)
    {
        try {
            ProtocolLibrary.getProtocolManager().recieveClientPacket(sender,
                                                                     getHandle());
        } catch (final Exception e) {
            throw new RuntimeException("Cannot recieve packet.", e);
        }
    }
}