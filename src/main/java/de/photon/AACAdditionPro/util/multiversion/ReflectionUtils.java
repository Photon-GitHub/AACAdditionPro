package de.photon.AACAdditionPro.util.multiversion;

import de.photon.AACAdditionPro.util.mathematics.AxisAlignedBB;
import de.photon.AACAdditionPro.util.reflection.ClassReflect;
import de.photon.AACAdditionPro.util.reflection.Reflect;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class ReflectionUtils
{
    private static final String versionNumber;

    static {
        versionNumber = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];
    }

    /**
     * Used to get the version {@link String} that is necessary for net.minecraft.server reflection
     *
     * @return e.g. v1_11_R1
     */
    public static String getVersionString()
    {
        return versionNumber;
    }

    public static List<AxisAlignedBB> getCollisionBoxes(Player player, AxisAlignedBB boundingBox)
    {
        // First we need a NMS bounding box
        Object nmsAxisAlignedBB = Reflect
                .fromNms("AxisAlignedBB")
                .constructor(double.class, double.class, double.class, double.class, double.class, double.class)
                .instance(boundingBox.getMaxX(), boundingBox.getMaxY(), boundingBox.getMaxZ(),
                        boundingBox.getMinX(), boundingBox.getMinY(), boundingBox.getMinZ() );

        // Now we need the NMS entity of the player (since the bot has none)
        Object nmsHandle = Reflect
                .fromObc("entity.CraftPlayer")
                .method("getHandle")
                .invoke(player);

        // Now we need to call getCubes(Entity, AxisAlignedBB) on the world
        Object nmsWorld = Reflect
                .fromObc("CraftWorld")
                .field("world")
                .from(player.getWorld())
                .as(Object.class);

        Object returnVal = Reflect
                .fromNms("World")
                .method("getCubes")
                .invoke(nmsWorld, nmsHandle, nmsAxisAlignedBB);

        // Now lets see what we got
        List<AxisAlignedBB> boxes = new ArrayList<>();
        List list = (List) returnVal;
        for ( Object nmsAABB : list ) {
            // nmsAABB is a NMS AxisAlignedBB
            boxes.add( AxisAlignedBB.fromNms(nmsAABB) );
        }

        return boxes;
    }

}
