package de.photon.aacadditionpro.util.world;

import com.google.common.base.Preconditions;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Contract;

/**
 * Provides util methods regarding {@link Location}s.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LocationUtil
{
    /**
     * Checks if two Entities are in the same world.
     */
    @Contract("null, _ -> fail; _, null -> fail")
    public static boolean inSameWorld(Entity entity1, Entity entity2)
    {
        return inSameWorld(entity1.getLocation(), entity2.getLocation());
    }

    @Contract("null, _ -> fail; _, null -> fail")
    public static boolean inSameWorld(Location locationOne, Location locationTwo)
    {
        return Preconditions.checkNotNull(locationOne.getWorld(), "NULL world in same world comparison (one)").getUID()
                            .equals(Preconditions.checkNotNull(locationTwo.getWorld(), "NULL world in same world comparison (two)").getUID());
    }

    /**
     * Simple method to know if a {@link Location} is close to another {@link Location}
     *
     * @param firstLocation   the first {@link Location}
     * @param secondLocation  the second {@link Location}
     * @param squaredDistance the squared distance that must be at most between the two {@link Location}s to make this {@link java.lang.reflect.Method} return true.
     *
     * @return true if the {@link Location} are in range, false if not
     */
    @Contract("null, _, _ -> fail; _, null, _ -> fail")
    public static boolean areLocationsInRange(final Location firstLocation, final Location secondLocation, final double squaredDistance)
    {
        return inSameWorld(firstLocation, secondLocation) &&
               firstLocation.distanceSquared(secondLocation) <= squaredDistance;
    }
}
