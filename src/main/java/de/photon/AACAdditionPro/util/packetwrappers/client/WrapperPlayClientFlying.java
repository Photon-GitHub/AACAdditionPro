package de.photon.AACAdditionPro.util.packetwrappers.client;


import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import de.photon.AACAdditionPro.util.packetwrappers.AbstractPacket;
import de.photon.AACAdditionPro.util.packetwrappers.IWrapperPlayOnGround;

public class WrapperPlayClientFlying extends AbstractPacket implements IWrapperPlayOnGround
{
    public static final PacketType TYPE = PacketType.Play.Client.FLYING;

    public WrapperPlayClientFlying()
    {
        super(new PacketContainer(TYPE), TYPE);
        handle.getModifier().writeDefaults();
    }

    public WrapperPlayClientFlying(PacketContainer packet)
    {
        super(packet, TYPE);
    }
}