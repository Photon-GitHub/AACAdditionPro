package de.photon.AACAdditionPro.util.packetwrappers;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import de.photon.AACAdditionPro.ServerVersion;

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

    public class MetadataBuilder
    {
        final WrappedDataWatcher dataWatcher = new WrappedDataWatcher();

        // ------------------------------------------------- Entity ------------------------------------------------- //

        /**
         * Sets a metadata.
         */
        public MetadataBuilder setMetadata(final int index, final Class classOfValue, final Object value)
        {
            switch (ServerVersion.getActiveServerVersion()) {
                case MC188:
                    dataWatcher.setObject(index, value);
                    break;
                case MC111:
                case MC112:
                case MC113:
                    dataWatcher.setObject(new WrappedDataWatcher.WrappedDataWatcherObject(index, WrappedDataWatcher.Registry.get(classOfValue)), value);
                    break;
                default:
                    throw new IllegalStateException("Unknown minecraft version");
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
                case MC188:
                    return this.setMetadata(6, Float.class, health);
                case MC111:
                case MC112:
                case MC113:
                    return this.setMetadata(7, Float.class, health);
                default:
                    throw new IllegalStateException("Unknown minecraft version");
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
                case MC188:
                    // IN 1.8.8 THIS IS A BYTE, NOT AN INTEGER!
                    return this.setMetadata(10, Byte.class, (byte) arrows);
                case MC111:
                case MC112:
                case MC113:
                    // IN 1.12.2 THIS IS AN INTEGER!
                    return this.setMetadata(10, Integer.class, arrows);
                default:
                    throw new IllegalStateException("Unknown minecraft version");
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
                case MC188:
                    return this.setMetadata(10, Byte.class, skinParts);
                case MC111:
                case MC112:
                case MC113:
                    return this.setMetadata(13, Byte.class, skinParts);
                default:
                    throw new IllegalStateException("Unknown minecraft version");
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
