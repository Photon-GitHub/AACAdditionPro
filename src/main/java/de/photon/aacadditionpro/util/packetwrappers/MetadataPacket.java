package de.photon.aacadditionpro.util.packetwrappers;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import de.photon.aacadditionpro.ServerVersion;
import de.photon.aacadditionpro.exception.UnknownMinecraftException;

import java.util.List;

public abstract class MetadataPacket extends AbstractPacket
{
    /**
     * Constructs a new strongly typed wrapper for the given packet.
     *
     * @param handle - handle to the raw packet data.
     * @param type   - the packet type.
     */
    protected MetadataPacket(PacketContainer handle, PacketType type)
    {
        super(handle, type);
    }

    public MetadataBuilder builder()
    {
        return new MetadataBuilder();
    }

    public static class MetadataBuilder
    {
        final WrappedDataWatcher dataWatcher = new WrappedDataWatcher();

        // ------------------------------------------------- Entity ------------------------------------------------- //

        /**
         * Sets a metadata.
         */
        public MetadataBuilder setMetadata(final int index, final Class classOfValue, final Object value)
        {
            switch (ServerVersion.getActiveServerVersion()) {
                case MC18:
                    dataWatcher.setObject(index, value);
                    break;
                case MC112:
                case MC113:
                case MC114:
                case MC115:
                case MC116:
                case MC117:
                    dataWatcher.setObject(new WrappedDataWatcher.WrappedDataWatcherObject(index, WrappedDataWatcher.Registry.get(classOfValue)), value);
                    break;
                default:
                    throw new UnknownMinecraftException();
            }

            return this;
        }

        /**
         * Sets the zero index metadata, which can be used for several settings.
         *
         * @see <a href="https://wiki.vg/Entity_metadata#Entity">https://wiki.vg/Entity_metadata#Entity</a>
         */
        public MetadataBuilder setZeroIndex(final byte zeroByte)
        {
            return this.setMetadata(0, Byte.class, zeroByte);
        }


        // ------------------------------------------------- Living ------------------------------------------------- //

        /**
         * Sets the health metadata.
         *
         * @see <a href="https://wiki.vg/Entity_metadata#Living">https://wiki.vg/Entity_metadata#Living</a>
         */
        public MetadataBuilder setHealthMetadata(final float health)
        {
            switch (ServerVersion.getActiveServerVersion()) {
                case MC18:
                    return this.setMetadata(6, Float.class, health);
                case MC112:
                case MC113:
                    return this.setMetadata(7, Float.class, health);
                case MC114:
                case MC115:
                case MC116:
                    return this.setMetadata(8, Float.class, health);
                case MC117:
                    return this.setMetadata(9, Float.class, health);
                default:
                    throw new UnknownMinecraftException();
            }
        }

        /**
         * Sets the arrows in entity metadata.
         *
         * @see <a href="https://wiki.vg/Entity_metadata#Living">https://wiki.vg/Entity_metadata#Living</a>
         */
        public MetadataBuilder setArrowInEntityMetadata(final int arrows)
        {
            switch (ServerVersion.getActiveServerVersion()) {
                case MC18:
                    // IN 1.8.8 THIS IS A BYTE, NOT AN INTEGER!
                    return this.setMetadata(10, Byte.class, (byte) arrows);
                case MC112:
                case MC113:
                    return this.setMetadata(10, Integer.class, arrows);
                case MC114:
                case MC115:
                case MC116:
                    return this.setMetadata(11, Integer.class, arrows);
                case MC117:
                    return this.setMetadata(12, Integer.class, arrows);
                default:
                    throw new UnknownMinecraftException();
            }
        }

        // ------------------------------------------------- Player ------------------------------------------------- //

        /**
         * Sets the skin part metadata
         *
         * @see <a href="https://wiki.vg/Entity_metadata#Player">https://wiki.vg/Entity_metadata#Player</a>
         */
        public MetadataBuilder setSkinMetadata(final byte skinParts)
        {
            switch (ServerVersion.getActiveServerVersion()) {
                case MC18:
                    return this.setMetadata(10, Byte.class, skinParts);
                case MC112:
                case MC113:
                case MC114:
                    return this.setMetadata(13, Byte.class, skinParts);
                case MC115:
                case MC116:
                    return this.setMetadata(16, Byte.class, skinParts);
                case MC117:
                    return this.setMetadata(17, Byte.class, skinParts);
                default:
                    throw new UnknownMinecraftException();
            }
        }

        /**
         * Get the metadata in form of a {@link WrappedDataWatcher}
         */
        public WrappedDataWatcher asWatcher()
        {
            return this.dataWatcher;
        }

        /**
         * Get the metadata in form of a {@link List} of {@link WrappedWatchableObject}s.
         */
        public List<WrappedWatchableObject> asList()
        {
            return this.dataWatcher.getWatchableObjects();
        }
    }
}
