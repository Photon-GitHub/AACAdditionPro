package de.photon.AACAdditionPro.util.packetwrappers.server;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import de.photon.AACAdditionPro.ServerVersion;

public class WrapperPlayServerRelEntityMoveLook extends WrapperPlayServerRelEntityMove implements IWrapperPlayServerLook
{
    public static final PacketType TYPE = PacketType.Play.Server.REL_ENTITY_MOVE_LOOK;

    public WrapperPlayServerRelEntityMoveLook()
    {
        super(new PacketContainer(TYPE), TYPE);
        handle.getModifier().writeDefaults();
    }

    public WrapperPlayServerRelEntityMoveLook(PacketContainer packet)
    {
        super(packet, TYPE);
    }

    @Override
    public int getByteOffset()
    {
        switch (ServerVersion.getActiveServerVersion()) {
            case MC188:
                return 3;
            case MC111:
            case MC112:
            case MC113:
                return 0;
            default:
                throw new IllegalStateException("Unknown minecraft version");
        }
    }

    @Override
    public boolean getOnGround()
    {
        return this.getHandle().getBooleans().read(0);
    }

    @Override
    public void setOnGround(final boolean value)
    {
        this.getHandle().getBooleans().write(0, value);
    }
}
