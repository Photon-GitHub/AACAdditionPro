package de.photon.aacadditionpro.modules.additions.esp;

import com.google.common.base.Preconditions;
import de.photon.aacadditionpro.util.mathematics.Hitbox;
import de.photon.aacadditionpro.util.mathematics.ResetLocation;
import de.photon.aacadditionpro.util.mathematics.ResetVector;
import de.photon.aacadditionpro.util.world.ChunkUtils;
import de.photon.aacadditionpro.util.world.InternalPotion;
import de.photon.aacadditionpro.util.world.MaterialUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.val;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
class CanSee
{
    // The real MAX_FOV is 110 (quake pro), which results in 150° according to tests.
    // 150° + 15° (compensation) = 165°
    public static final double MAX_FOV = Math.toRadians(165D);

    /**
     * Determines whether a {@link Player} can see another {@link Player}
     */
    public static boolean canSee(Player observer, Player watched)
    {
        // Glowing.

        if (InternalPotion.GLOWING.hasPotionEffect(watched)) return true;

        // ----------------------------------- Calculation ---------------------------------- //

        final Vector[] cameraVectors = Esp.CAMERA_VECTOR_SUPPLIER.apply(observer);

        // Get the Vectors of the hitbox to check.
        final Hitbox hitbox = watched.isSneaking() ? Hitbox.ESP_SNEAKING_PLAYER : Hitbox.ESP_PLAYER;
        final Vector[] watchedHitboxVectors = hitbox.getLowResolutionCalculationVectors(watched.getLocation());

        // The needed variables for the calculation.
        // Use ResetLocation to reduce the amount of object creations to a minimum.
        final ResetLocation cameraLocation = new ResetLocation();
        // Another ResetLocation for a computation to reduce the amount of clone() operation.
        final ResetLocation cameraLocationPlusBetween = new ResetLocation();
        final ResetVector observerLocationVector = new ResetVector(observer.getLocation().toVector());

        Vector between;
        for (final Vector cameraVector : cameraVectors) {
            cameraLocation.setBaseLocation(cameraVector.toLocation(observer.getWorld()));
            // No cloning of baseLocation necessary here as the baseLocation is never changed.
            cameraLocationPlusBetween.setBaseLocation(cameraLocation.getBaseLocation());

            for (final Vector destinationVector : watchedHitboxVectors) {
                cameraLocation.resetToBase();

                // The resulting Vector
                // The camera is not blocked by non-solid blocks
                // Vector is intersecting with some blocks
                //
                // Cloning IS needed as we are in a second loop.
                between = destinationVector.clone().subtract(cameraVector);

                // ---------------------------------------------- FOV ----------------------------------------------- //

                // Subtract the wrong way around and multiply with -1 to avoid .clone() operation.
                if (observerLocationVector.resetToBase().subtract(cameraVector).multiply(-1).angle(between) > MAX_FOV) continue;

                // ---------------------------------------- Cache Calculation --------------------------------------- //

                // Make sure the chunks are loaded.
                // If the chunks are not loaded assume the players can see each other.
                if (!ChunkUtils.areChunksLoadedBetweenLocations(cameraLocation, cameraLocationPlusBetween.resetToBase().add(between))) return true;

                // --------------------------------------- Normal Calculation --------------------------------------- //
                // No intersection found
                if (checkRay(cameraLocation, between, destinationVector)) return true;
            }
        }

        return false;
    }

    private static boolean checkRay(ResetLocation cameraLocation, Vector betweenVector, Vector destinationVector)
    {
        val dest = new ResetLocation(cameraLocation.getWorld(), destinationVector.getX(), destinationVector.getY(), destinationVector.getZ());
        val between = new ResetVector(betweenVector);

        Block block;
        for (double d : heuristicScalars(cameraLocation.distance(dest))) {
            block = cameraLocation.add(between.multiply(d)).getBlock();
            //noinspection ConstantConditions
            if (block != null && !block.isEmpty() && MaterialUtil.isReallyOccluding(block.getType())) return false;
            cameraLocation.resetToBase();
            between.resetToBase();
        }
        return true;
    }

    private static double[] heuristicScalars(double distance)
    {
        if (distance <= 2) return new double[]{1};
        if (distance <= 3) return new double[]{1, distance - 1};
        if (distance <= 5) return new double[]{1, 1.5, 2, distance - 2, distance - 1.5, distance - 1};
        if (distance <= 10) return new double[]{1, 1.5, 2, 2.5, distance / 2, distance - 2.5, distance - 2, distance - 1.5, distance - 1};
        if (distance <= 25) return new double[]{1, 1.5, 2, 2.5, distance / 4, distance / 2, 3 * distance / 4, distance - 2.5, distance - 2, distance - 1.5, distance - 1};
        return new double[]{1, 1.5, 2, 2.5, distance / 4, distance / 3, distance / 2, 2 * distance / 3, 3 * distance / 4, distance - 2.5, distance - 2, distance - 1.5, distance - 1};
    }

    /**
     * Get to know where the {@link Vector} intersects with a {@link org.bukkit.block.Block}.
     * Non-Occluding {@link Block}s as defined in {@link MaterialUtil#isReallyOccluding(Material)} are ignored.
     *
     * @param start     the starting {@link Location}
     * @param direction the {@link Vector} which should be checked
     *
     * @return The length when the {@link Vector} intersects or 0 if no intersection was found
     */
    public static double getDistanceToFirstIntersectionWithBlock(final Location start, final Vector direction)
    {
        Preconditions.checkNotNull(start.getWorld(), "RayTrace: Unknown start world.");
        val length = (int) direction.length();

        if (length >= 1) {
            try {
                val blockIterator = new BlockIterator(start.getWorld(), start.toVector(), direction, 0, length);
                Block block;
                while (blockIterator.hasNext()) {
                    block = blockIterator.next();
                    // Account for a Spigot bug: BARRIER and MOB_SPAWNER are not occluding blocks
                    // Use the middle location of the Block instead of the simple location.
                    if (MaterialUtil.isReallyOccluding(block.getType())) return block.getLocation().clone().add(0.5, 0.5, 0.5).distance(start);
                }
            } catch (IllegalStateException exception) {
                // Just in case the start block could not be found for some reason or a chunk is loaded async.
                return 0;
            }
        }
        return 0;
    }
}
