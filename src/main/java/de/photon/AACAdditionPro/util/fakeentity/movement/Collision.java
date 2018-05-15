package de.photon.AACAdditionPro.util.fakeentity.movement;


import de.photon.AACAdditionPro.util.mathematics.AxisAlignedBB;
import de.photon.AACAdditionPro.util.mathematics.Hitbox;
import de.photon.AACAdditionPro.util.reflection.ReflectionUtils;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import java.util.List;

public final class Collision
{
    /**
     * Looks for collisions that could occur during the movement and adds them to the initial {@link Location} to get an uncollided {@link Location}
     *
     * @param dependantEntity the Entity used for the reflection process
     * @param input           the initial {@link Location}
     * @param hitbox          the {@link Hitbox} of the used {@link org.bukkit.entity.Entity}
     * @param velocity        the planned movement
     *
     * @return the vector to add to the input for the nearest, uncollided {@link Location}
     */
    public static Vector getNearestUncollidedLocation(Entity dependantEntity, Location input, Hitbox hitbox, Vector velocity)
    {
        // Do not touch the real velocity of the entity.
        velocity = velocity.clone();

        // Construct the BoundingBox
        final AxisAlignedBB bb = hitbox.constructBoundingBox(input);

        // Get the collisions
        final List<AxisAlignedBB> collisions = ReflectionUtils.getCollisionBoxes(dependantEntity, bb
                // Add the scheduled movement. This DOES NOT MODIFY INTERNAL VALUES, only call this for the Reflection!!!
                .addCoordinatesToNewBox(velocity.getX(), velocity.getY(), velocity.getZ()));

        // Check if we would hit a y border block
        for (AxisAlignedBB collisionBox : collisions) {
            velocity.setY(collisionBox.calculateYOffset(bb, velocity.getY()));
        }

        bb.offset(0, velocity.getY(), 0);

        // Check if we would hit a x border block
        for (AxisAlignedBB collisionBox : collisions) {
            velocity.setX(collisionBox.calculateXOffset(bb, velocity.getX()));
        }

        bb.offset(velocity.getX(), 0, 0);

        // Check if we would hit a z border block
        for (AxisAlignedBB collisionBox : collisions) {
            velocity.setZ(collisionBox.calculateZOffset(bb, velocity.getZ()));
        }

        // No offset here as the bb is not used anymore afterwards.

        // Returns the cloned input with the needed offset.
        return velocity;
    }
}