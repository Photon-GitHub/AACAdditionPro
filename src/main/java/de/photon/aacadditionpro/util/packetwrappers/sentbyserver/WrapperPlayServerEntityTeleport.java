package de.photon.aacadditionpro.util.packetwrappers.sentbyserver;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import de.photon.aacadditionpro.ServerVersion;
import de.photon.aacadditionpro.exception.UnknownMinecraftException;
import de.photon.aacadditionpro.util.packetwrappers.AbstractPacket;
import de.photon.aacadditionpro.util.packetwrappers.IWrapperPlayPosition;
import org.bukkit.Location;

public class WrapperPlayServerEntityTeleport extends AbstractPacket implements IWrapperPlayServerLook, IWrapperPlayPosition
{
    public static final PacketType TYPE = PacketType.Play.Server.ENTITY_TELEPORT;

    public WrapperPlayServerEntityTeleport()
    {
        super(new PacketContainer(TYPE), TYPE);
        handle.getModifier().writeDefaults();
    }

    public WrapperPlayServerEntityTeleport(PacketContainer packet)
    {
        super(packet, TYPE);
    }

    @Override
    public void setWithLocation(Location location)
    {
        this.setX(location.getX());
        this.setY(location.getY());
        this.setZ(location.getZ());

        this.setYaw(location.getYaw());
        this.setPitch(location.getPitch());
    }

    @Override
    public double getX()
    {
        switch (ServerVersion.getActiveServerVersion()) {
            case MC18:
                return handle.getIntegers().read(1) / 32.0;
            case MC112:
            case MC113:
            case MC114:
            case MC115:
            case MC116:
            case MC117:
                return handle.getDoubles().read(0);
            default:
                throw new UnknownMinecraftException();
        }
    }

    @Override
    public void setX(double value)
    {
        switch (ServerVersion.getActiveServerVersion()) {
            case MC18:
                handle.getIntegers().write(1, (int) value * 32);
                break;
            case MC112:
            case MC113:
            case MC114:
            case MC115:
            case MC116:
            case MC117:
                handle.getDoubles().write(0, value);
                break;
            default:
                throw new UnknownMinecraftException();
        }
    }

    @Override
    public double getY()
    {
        switch (ServerVersion.getActiveServerVersion()) {
            case MC18:
                return handle.getIntegers().read(2) / 32.0;
            case MC112:
            case MC113:
            case MC114:
            case MC115:
            case MC116:
            case MC117:
                return handle.getDoubles().read(1);
            default:
                throw new UnknownMinecraftException();
        }
    }

    @Override
    public void setY(double value)
    {
        switch (ServerVersion.getActiveServerVersion()) {
            case MC18:
                handle.getIntegers().write(2, (int) value * 32);
                break;
            case MC112:
            case MC113:
            case MC114:
            case MC115:
            case MC116:
            case MC117:
                handle.getDoubles().write(1, value);
                break;
            default:
                throw new UnknownMinecraftException();
        }
    }

    @Override
    public double getZ()
    {
        switch (ServerVersion.getActiveServerVersion()) {
            case MC18:
                return handle.getIntegers().read(3) / 32.0;
            case MC112:
            case MC113:
            case MC114:
            case MC115:
            case MC116:
            case MC117:
                return handle.getDoubles().read(2);
            default:
                throw new UnknownMinecraftException();
        }
    }

    @Override
    public void setZ(double value)
    {
        switch (ServerVersion.getActiveServerVersion()) {
            case MC18:
                handle.getIntegers().write(3, (int) value * 32);
                break;
            case MC112:
            case MC113:
            case MC114:
            case MC115:
            case MC116:
            case MC117:
                handle.getDoubles().write(2, value);
                break;
            default:
                throw new UnknownMinecraftException();
        }
    }
}
