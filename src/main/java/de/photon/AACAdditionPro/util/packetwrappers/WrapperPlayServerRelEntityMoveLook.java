package de.photon.AACAdditionPro.util.packetwrappers;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import de.photon.AACAdditionPro.AACAdditionPro;

public class WrapperPlayServerRelEntityMoveLook extends WrapperPlayServerRelEntityMove implements IWrapperPlayServerEntityLook
{
    public static final PacketType TYPE =
            PacketType.Play.Server.REL_ENTITY_MOVE_LOOK;

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
        switch (AACAdditionPro.getInstance().getServerVersion()) {
            case MC188:
                return 3;
            case MC110:
            case MC111:
            case MC112:
                return 0;
            default:
                throw new IllegalStateException("Unknown minecraft version");
        }
    }
}