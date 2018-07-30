package de.photon.AACAdditionPro.util.mathematics;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Getter
public enum Hitbox
{
    /**
     * The normal hitbox of a player
     */
    PLAYER(0.3D, 0.3D, 1.8D),
    /**
     * The hitbox of a sneaking player
     */
    SNEAKING_PLAYER(0.3D, 0.3D, 1.65D),
    /**
     * A hitbox that covers the whole body (which is partially outside the normal hitbox)
     */
    ESP_PLAYER(0.5, 0.5, 1.8D),
    /**
     * A hitbox that covers the whole body (which is partially outside the normal hitbox)while the player is sneaking
     */
    ESP_SNEAKING_PLAYER(0.5, 0.5, 1.65D);

    private final double offsetX;
    private final double offsetZ;
    private final double height;

    /**
     * Creates an array of {@link Vector}s that indicates essential positions of the hitbox which are helpful for
     * raytracing.
     *
     * @param location         the initial {@link Location} of the {@link org.bukkit.entity.Entity}, thus the basis of the {@link Hitbox}.
     * @param addCenterVectors whether only the {@link Vector}s of the corners should be returned in the {@link List} or additional {@link Vector}s
     *                         in the center of the {@link org.bukkit.entity.Entity} (alongside the y-axis) should be added
     *
     * @return an array of all the constructed {@link Vector}s.
     */
    public Vector[] getCalculationVectors(final Location location, final boolean addCenterVectors)
    {
        final List<Vector> vectors = new ArrayList<>(13);

        final double lowerY = location.getY();
        final double upperY = lowerY + this.height;

        //Lower corners
        vectors.add(new Vector(location.getX() + this.offsetX, lowerY, location.getZ() + this.offsetZ));
        vectors.add(new Vector(location.getX() - this.offsetX, lowerY, location.getZ() + this.offsetZ));
        vectors.add(new Vector(location.getX() + this.offsetX, lowerY, location.getZ() - this.offsetZ));
        vectors.add(new Vector(location.getX() - this.offsetX, lowerY, location.getZ() - this.offsetZ));

        //Upper corners
        vectors.add(new Vector(location.getX() + this.offsetX, upperY, location.getZ() + this.offsetZ));
        vectors.add(new Vector(location.getX() - this.offsetX, upperY, location.getZ() + this.offsetZ));
        vectors.add(new Vector(location.getX() + this.offsetX, upperY, location.getZ() - this.offsetZ));
        vectors.add(new Vector(location.getX() - this.offsetX, upperY, location.getZ() - this.offsetZ));

        if (addCenterVectors)
        {
            Vector start = location.toVector();
            while (start.getY() < upperY)
            {
                vectors.add(start);
                // 0.47 as a factor as of slabs and other irregular block models.
                start = start.clone().setY(start.getY() + 0.47);
            }
        }
        return vectors.toArray(new Vector[0]);
    }

    /**
     * Constructs an {@link AxisAlignedBB} on the basis of the provided {@link Location}
     *
     * @param location the {@link Location} to base the bounding box on.
     */
    public AxisAlignedBB constructBoundingBox(final Location location)
    {
        return new AxisAlignedBB(
                location.getX() - this.offsetX,
                // The location is based on the feet location
                location.getY(),
                location.getZ() - this.offsetZ,

                location.getX() + this.offsetX,
                // The location is based on the feet location
                location.getY() + this.height,
                location.getZ() + this.offsetZ
        );
    }
}
