package de.photon.AACAdditionPro.util.world;

import com.google.common.collect.ImmutableList;
import de.photon.AACAdditionPro.util.mathematics.Hitbox;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public final class BlockUtils
{

    private static final List<BlockFace> horizontalFaces = ImmutableList.of(
            BlockFace.NORTH,
            BlockFace.SOUTH,
            BlockFace.WEST,
            BlockFace.EAST);
    private static final List<BlockFace> allFaces = ImmutableList.of(
            BlockFace.UP,
            BlockFace.DOWN,
            BlockFace.NORTH,
            BlockFace.SOUTH,
            BlockFace.WEST,
            BlockFace.EAST);

    /**
     * Whether an entity should jump if collided horizontally against that material.
     */
    public static boolean isJumpMaterial(Material material)
    {
        return material.isSolid();
    }

    /**
     * Checks if a {@link Hitbox} at a certain {@link Location} is inside liquids.
     *
     * @param location the {@link Location} to base the {@link Hitbox} on.
     * @param hitbox   the type of {@link Hitbox} that should be constructed.
     */
    public static boolean isHitboxInLiquids(Location location, Hitbox hitbox)
    {
        return isHitboxInMaterials(location, hitbox, Arrays.asList(Material.WATER, Material.LAVA, Material.STATIONARY_WATER, Material.STATIONARY_LAVA));
    }

    /**
     * Checks if a {@link Hitbox} at a certain {@link Location} is inside of one of the provided {@link Material}s.
     *
     * @param location  the {@link Location} to base the {@link Hitbox} on.
     * @param hitbox    the type of {@link Hitbox} that should be constructed.
     * @param materials the {@link Material}s that should be checked for.
     */
    public static boolean isHitboxInMaterials(Location location, Hitbox hitbox, Collection<Material> materials)
    {
        Iterable<Vector> vectors = hitbox.getCalculationVectors(location, false);
        // The max. amount of blocks that could be in the collection is 8
        // Use a check-if present method as getBlock calls are performance-intensive.
        Collection<int[]> checkedBlockLocations = new ArrayList<>(8);

        for (Vector vector : vectors) {
            final int[] blockCoordsOfVector = new int[]{
                    vector.getBlockX(),
                    vector.getBlockY(),
                    vector.getBlockZ()
            };

            // If the location was already checked go further.
            if (!checkedBlockLocations.contains(blockCoordsOfVector)) {
                if (materials.contains(location.getWorld().getBlockAt(blockCoordsOfVector[0], blockCoordsOfVector[1], blockCoordsOfVector[2]).getType())) {
                    return true;
                }

                checkedBlockLocations.add(blockCoordsOfVector);
            }
        }
        return false;
    }

    /**
     * This can be used to know if the {@link Block}s are next to each other.
     *
     * @param a              the first {@link Block}
     * @param b              the second {@link Block}
     * @param onlyHorizontal whether only the horizontal {@link BlockFace}s should be checked for the second {@link Block} or all {@link BlockFace}s (horizontal + vertical)
     *
     * @return true if the {@link Block}s are next to each other, or false if they are not
     */
    public static boolean isNext(final Block a, final Block b, final boolean onlyHorizontal)
    {
        if (!a.getWorld().equals(b.getWorld())) {
            return false;
        }

        for (final BlockFace face : onlyHorizontal ?
                                    horizontalFaces :
                                    allFaces) {
            if (a.getRelative(face).equals(b)) {
                return true;
            }
        }
        return false;
    }

    /**
     * This counts the {@link Block}s around the given block if they are not air/empty.
     *
     * @param block          the block that faces should be checked for other {@link Block}s
     * @param onlyHorizontal whether only the {@link Block}s should be counted that are horizontal around the block or all {@link Block}s (horizontal + vertical)
     *
     * @return the amount of {@link Block}s which were counted
     */
    public static byte blocksAround(final Block block, final boolean onlyHorizontal)
    {
        byte count = 0;
        for (final BlockFace f : onlyHorizontal ?
                                 horizontalFaces :
                                 allFaces) {
            if (!block.getRelative(f).isEmpty()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Fix for Spigot's broken occluding method.
     */
    public static boolean isReallyOccluding(Material material)
    {
        return material.isOccluding() && material != Material.BARRIER && material != Material.MOB_SPAWNER;
    }
}
