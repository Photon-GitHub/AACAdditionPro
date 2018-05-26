package de.photon.AACAdditionPro.util.world;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import de.photon.AACAdditionPro.util.mathematics.AxisAlignedBB;
import de.photon.AACAdditionPro.util.mathematics.Hitbox;
import de.photon.AACAdditionPro.util.mathematics.MathUtils;
import de.photon.AACAdditionPro.util.multiversion.ServerVersion;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public final class BlockUtils
{
    public static final Set<Material> LIQUIDS = ImmutableSet.of(Material.WATER, Material.LAVA, Material.STATIONARY_WATER, Material.STATIONARY_LAVA);
    public static final Set<Material> CONTAINERS;

    static
    {
        switch (ServerVersion.getActiveServerVersion())
        {
            case MC188:
            case MC110:
                CONTAINERS = ImmutableSet.of(Material.CHEST,
                                             Material.TRAPPED_CHEST,
                                             Material.ENDER_CHEST,
                                             Material.ANVIL,
                                             Material.FURNACE,
                                             Material.DISPENSER,
                                             Material.DROPPER);
                break;
                
            case MC111:
            case MC112:
                CONTAINERS = ImmutableSet.of(Material.CHEST,
                                             Material.TRAPPED_CHEST,
                                             Material.ENDER_CHEST,
                                             Material.ANVIL,
                                             Material.FURNACE,
                                             Material.DISPENSER,
                                             Material.DROPPER,
                                             Material.BLACK_SHULKER_BOX,
                                             Material.BROWN_SHULKER_BOX,
                                             Material.BLUE_SHULKER_BOX,
                                             Material.CYAN_SHULKER_BOX,
                                             Material.GRAY_SHULKER_BOX,
                                             Material.GREEN_SHULKER_BOX,
                                             Material.LIGHT_BLUE_SHULKER_BOX,
                                             Material.LIME_SHULKER_BOX,
                                             Material.MAGENTA_SHULKER_BOX,
                                             Material.ORANGE_SHULKER_BOX,
                                             Material.PINK_SHULKER_BOX,
                                             Material.PURPLE_SHULKER_BOX,
                                             Material.RED_SHULKER_BOX,
                                             Material.SILVER_SHULKER_BOX,
                                             Material.WHITE_SHULKER_BOX,
                                             Material.YELLOW_SHULKER_BOX);
                break;
            default:
                throw new IllegalStateException("Unknown minecraft version");
        }


    }

    public static final List<BlockFace> HORIZONTAL_FACES = ImmutableList.of(
            BlockFace.NORTH,
            BlockFace.SOUTH,
            BlockFace.WEST,
            BlockFace.EAST);

    public static final List<BlockFace> ALL_FACES = ImmutableList.of(
            BlockFace.UP,
            BlockFace.DOWN,
            BlockFace.NORTH,
            BlockFace.SOUTH,
            BlockFace.WEST,
            BlockFace.EAST);

    /**
     * This method finds the next free space to a {@link Location} if only searching on the y - Axis.
     *
     * @return the {@link Location} of the closest free space found or a {@link Location} of y = 260 if no free space was found.
     */
    public static Location getClosestFreeSpaceYAxis(final Location location, final Hitbox hitbox)
    {
        // Short as no hitbox is larger than 32k blocks.
        // Represents the needed empty blocks, slabs (or other non-full blocks) are not included
        final short neededHeight = (short) Math.ceil(hitbox.getHeight());

        // The offset of the next free space to the location.
        double minDeltaY = Double.MAX_VALUE;

        // Set to 260 as that is the default value if nothing else is found.
        double currentY = 260;

        final BlockIterator blockIterator = new BlockIterator(location.getWorld(),
                                                              location.toVector(),
                                                              // From the sky to the void to have less needed calculations
                                                              new Vector(0, -1, 0),
                                                              // Add 20 to check both over and below the starting location.
                                                              20,
                                                              (int) Math.min(
                                                                      // Make sure the BlockIterator will not iterate into the void.
                                                                      location.getY() + 20,
                                                                      // 40 as default length.
                                                                      40));

        short currentHeight = 0;
        Block currentBlock;

        while (blockIterator.hasNext())
        {
            currentBlock = blockIterator.next();

            final double originOffset = MathUtils.offset(location.getY(), currentBlock.getY());

            // >= To prefer "higher" positions and improve performance.
            if (originOffset >= minDeltaY)
            {
                // Now we can only get worse results.
                break;
            }

            // Check if the block is empty
            if (currentBlock.isEmpty())
            {
                // If the empty space is big enough
                if (++currentHeight >= neededHeight)
                {
                    minDeltaY = originOffset;
                    currentY = currentBlock.getY();
                }
            }
            else
            {
                currentHeight = 0;
            }
        }

        final Location spawnLocation = location.clone();
        spawnLocation.setY(currentY);
        return spawnLocation;
    }

    /**
     * Checks if a {@link Hitbox} at a certain {@link Location} is inside liquids.
     *
     * @param location the {@link Location} to base the {@link Hitbox} on.
     * @param hitbox   the type of {@link Hitbox} that should be constructed.
     */
    public static boolean isHitboxInLiquids(final Location location, final Hitbox hitbox)
    {
        return isHitboxInMaterials(location, hitbox, LIQUIDS);
    }

    /**
     * Checks if a {@link Hitbox} at a certain {@link Location} is inside of one of the provided {@link Material}s.
     *
     * @param location  the {@link Location} to base the {@link Hitbox} on.
     * @param hitbox    the type of {@link Hitbox} that should be constructed.
     * @param materials the {@link Material}s that should be checked for.
     */
    public static boolean isHitboxInMaterials(final Location location, final Hitbox hitbox, final Collection<Material> materials)
    {
        final AxisAlignedBB axisAlignedBB = hitbox.constructBoundingBox(location);

        // Cast the first value as that will only make it smaller, the second one has to be ceiled as it could be the same value once again.
        for (int x = (int) axisAlignedBB.getMinX(); x <= (int) Math.ceil(axisAlignedBB.getMaxX()); x++)
        {
            for (int y = (int) axisAlignedBB.getMinY(); y <= (int) Math.ceil(axisAlignedBB.getMaxY()); y++)
            {
                for (int z = (int) axisAlignedBB.getMinZ(); z <= (int) Math.ceil(axisAlignedBB.getMaxZ()); z++)
                {
                    if (materials.contains(location.getWorld().getBlockAt(x, y, z).getType()))
                    {
                        return true;
                    }
                }
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
        return a.getWorld().equals(b.getWorld()) && (onlyHorizontal ?
                                                     HORIZONTAL_FACES.contains(a.getFace(b)) :
                                                     ALL_FACES.contains(a.getFace(b)));
    }

    /**
     * This counts the {@link Block}s around the given block if they are not air/empty.
     *
     * @param block          the block that faces should be checked for other {@link Block}s
     * @param onlyHorizontal whether only the {@link Block}s should be counted that are horizontal around the block or all {@link Block}s (horizontal + vertical)
     *
     * @return the amount of {@link Block}s which were counted
     */
    public static byte countBlocksAround(final Block block, final boolean onlyHorizontal)
    {
        byte count = 0;
        for (final BlockFace f : onlyHorizontal ?
                                 HORIZONTAL_FACES :
                                 ALL_FACES)
        {
            if (!block.getRelative(f).isEmpty())
            {
                count++;
            }
        }
        return count;
    }

    /**
     * This gets the {@link Block}s around the given block if they are not air/empty.
     *
     * @param block          the block that faces should be checked for other {@link Block}s
     * @param onlyHorizontal whether only the {@link Block}s should be counted that are horizontal around the block or all {@link Block}s (horizontal + vertical)
     *
     * @return a {@link List} of all {@link Block}s which were found.
     */
    public static List<Block> getBlocksAround(final Block block, final boolean onlyHorizontal)
    {
        final List<Block> blocks = new ArrayList<>(onlyHorizontal ? 4 : 6);
        for (final BlockFace face : onlyHorizontal ?
                                    HORIZONTAL_FACES :
                                    ALL_FACES)
        {
            final Block relative = block.getRelative(face);
            if (!relative.isEmpty())
            {
                blocks.add(relative);
            }
        }
        return blocks;
    }

    /**
     * Fix for Spigot's broken occluding method.
     */
    public static boolean isReallyOccluding(Material material)
    {
        return material.isOccluding() && material != Material.BARRIER && material != Material.MOB_SPAWNER;
    }
}
