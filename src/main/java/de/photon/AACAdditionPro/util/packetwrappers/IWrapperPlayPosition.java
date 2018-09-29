package de.photon.AACAdditionPro.util.packetwrappers;

import org.bukkit.Location;

public interface IWrapperPlayPosition extends IWrapperPlay
{
    /**
     * Retrieve X.
     * <p>
     * Notes: absolute position
     *
     * @return The current X
     */
    default double getX()
    {
        return getHandle().getDoubles().read(0);
    }

    /**
     * Set X.
     *
     * @param value - new value.
     */
    default void setX(final double value)
    {
        getHandle().getDoubles().write(0, value);
    }

    /**
     * Retrieve Feet Y.
     * <p>
     * Notes: absolute feet position. Is normally HeadY - 1.62. Used to modify
     * the players bounding box when going up stairs, crouching, etc…
     *
     * @return The current FeetY
     */
    default double getY()
    {
        return getHandle().getDoubles().read(1);
    }

    /**
     * Set Feet Y.
     *
     * @param value - new value.
     */
    default void setY(final double value)
    {
        getHandle().getDoubles().write(1, value);
    }

    /**
     * Retrieve Z.
     * <p>
     * Notes: absolute position
     *
     * @return The current Z
     */
    default double getZ()
    {
        return getHandle().getDoubles().read(2);
    }

    /**
     * Set Z.
     *
     * @param value - new value.
     */
    default void setZ(final double value)
    {
        getHandle().getDoubles().write(2, value);
    }

    default void setWithLocation(Location location)
    {
        this.setX(location.getX());
        this.setY(location.getY());
        this.setZ(location.getZ());
    }
}
