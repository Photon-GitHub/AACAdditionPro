package de.photon.AACAdditionPro.util.packetwrappers.server;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.google.common.collect.ImmutableSet;
import de.photon.AACAdditionPro.util.packetwrappers.AbstractPacket;
import de.photon.AACAdditionPro.util.packetwrappers.IWrapperPlayLook;
import de.photon.AACAdditionPro.util.packetwrappers.IWrapperPlayPosition;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Collections;
import java.util.Set;

public class WrapperPlayServerPosition extends AbstractPacket implements IWrapperPlayPosition, IWrapperPlayLook
{
    public static final PacketType TYPE = PacketType.Play.Server.POSITION;

    public WrapperPlayServerPosition()
    {
        super(new PacketContainer(TYPE), TYPE);
        handle.getModifier().writeDefaults();
    }

    public WrapperPlayServerPosition(final PacketContainer packet)
    {
        super(packet, TYPE);
    }

    @Override
    public float getYaw()
    {
        return handle.getFloat().read(0);
    }

    @Override
    public void setYaw(final float value)
    {
        handle.getFloat().write(0, value);
    }

    @Override
    public float getPitch()
    {
        return handle.getFloat().read(1);
    }

    @Override
    public void setPitch(final float value)
    {
        handle.getFloat().write(1, value);
    }

    /**
     * Constructs a new {@link Location} with the information of this packet.
     */
    public Location getLocation(final World world)
    {
        return new Location(world, this.getX(), this.getY(), this.getZ(), this.getYaw(), this.getPitch());
    }

    @Override
    public void setWithLocation(final Location location)
    {
        this.setX(location.getX());
        this.setY(location.getY());
        this.setZ(location.getZ());

        this.setYaw(location.getYaw());
        this.setPitch(location.getPitch());
    }

    private static final Class<?> FLAGS_CLASS = MinecraftReflection.getMinecraftClass(
            "EnumPlayerTeleportFlags",
            "PacketPlayOutPosition$EnumPlayerTeleportFlags");

    public enum PlayerTeleportFlag
    {
        X,
        Y,
        Z,
        Y_ROT,
        X_ROT;

        private final static Set<PlayerTeleportFlag> ALL_FLAGS = ImmutableSet.copyOf(PlayerTeleportFlag.values());
    }

    private StructureModifier<Set<PlayerTeleportFlag>> getFlagsModifier()
    {
        return handle.getSets(EnumWrappers.getGenericConverter(FLAGS_CLASS, PlayerTeleportFlag.class));
    }

   /* private StructureModifier<Set<PlayerTeleportFlag>> getFlagsModifier()
    {
        return handle.getModifier().withType(
                Set.class,
                BukkitConverters.getSetConverter(FLAGS_CLASS, EnumWrappers
                        .getGenericConverter(PlayerTeleportFlag.class)));
    }*/

    public Set<PlayerTeleportFlag> getFlags()
    {
        return getFlagsModifier().read(0);
    }

    public void setFlags(final Set<PlayerTeleportFlag> value)
    {
        getFlagsModifier().write(0, value);
    }

    /**
     * Sets no relative movement flags.
     */
    public void setNoFlags()
    {
        this.setFlags(Collections.emptySet());
    }

    /**
     * Sets all relative movement flags of this packet, i.e.:
     * {@link PlayerTeleportFlag#X}, <br>
     * {@link PlayerTeleportFlag#Y}, <br>
     * {@link PlayerTeleportFlag#Z}, <br>
     * {@link PlayerTeleportFlag#X_ROT}, <br>
     * {@link PlayerTeleportFlag#Y_ROT}
     */
    public void setAllFlags()
    {
        this.setFlags(PlayerTeleportFlag.ALL_FLAGS);
    }
}