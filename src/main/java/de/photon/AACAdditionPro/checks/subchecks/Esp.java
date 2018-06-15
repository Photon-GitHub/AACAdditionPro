package de.photon.AACAdditionPro.checks.subchecks;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.ModuleType;
import de.photon.AACAdditionPro.checks.ViolationModule;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.user.UserManager;
import de.photon.AACAdditionPro.util.files.configs.Configs;
import de.photon.AACAdditionPro.util.files.configs.LoadFromConfiguration;
import de.photon.AACAdditionPro.util.mathematics.AxisAlignedBB;
import de.photon.AACAdditionPro.util.mathematics.Hitbox;
import de.photon.AACAdditionPro.util.mathematics.ParallelBasePyramidRectangle;
import de.photon.AACAdditionPro.util.mathematics.VectorUtils;
import de.photon.AACAdditionPro.util.multiversion.ServerVersion;
import de.photon.AACAdditionPro.util.visibility.HideMode;
import de.photon.AACAdditionPro.util.visibility.PlayerInformationModifier;
import de.photon.AACAdditionPro.util.visibility.informationmodifiers.InformationObfuscator;
import de.photon.AACAdditionPro.util.visibility.informationmodifiers.PlayerHider;
import de.photon.AACAdditionPro.util.world.BlockUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class Esp implements ViolationModule
{
    // The auto-config-data
    private double renderDistanceSquared = 0;
    private boolean hideAfterRenderDistance = true;

    @LoadFromConfiguration(configPath = ".update_ticks")
    private int update_ticks;

    // The camera offset for 3rd person
    private static final double THIRD_PERSON_OFFSET = 5D;

    // The real MAX_FOV is 110 (quake pro), which results in 150° according to tests.
    // 150° + 15° (compensation) = 165°
    private static final double MAX_FOV = Math.toRadians(165D);

    // Use a LinkedList design for optimal storage usage as the amount of bypassed / spectator players cannot be estimated.
    private final Deque<Pair> playerConnections = new ArrayDeque<>();

    private final PlayerInformationModifier fullHider = new PlayerHider();
    private final PlayerInformationModifier informationOnlyHider = new InformationObfuscator();


    // The task number for Bukkit's internal systems
    private int taskNumber;

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.ESP;
    }

    @Override
    public void subEnable()
    {
        // ---------------------------------------------------- Auto-configuration ----------------------------------------------------- //

        final YamlConfiguration spigot = Configs.SPIGOT.getConfigurationRepresentation().getYamlConfiguration();
        final ConfigurationSection worlds = spigot.getConfigurationSection("world-settings");

        for (final String world : worlds.getKeys(false))
        {
            int currentPlayerTrackingRange = spigot.getInt(worlds.getCurrentPath() + "." + world + ".entity-tracking-range.players");

            // Square
            currentPlayerTrackingRange *= currentPlayerTrackingRange;

            if (currentPlayerTrackingRange > renderDistanceSquared)
            {
                renderDistanceSquared = currentPlayerTrackingRange;

                // Do the maths inside here as reading from a file takes longer than calculating this.
                // 19321 == 139^2 as of the maximum range of the block-iterator
                if (renderDistanceSquared > 19321)
                {
                    hideAfterRenderDistance = false;
                    renderDistanceSquared = 19321;
                    break;
                }
            }
        }

        // ----------------------------------------------------------- Task ------------------------------------------------------------ //

        taskNumber = Bukkit.getScheduler().scheduleSyncRepeatingTask(
                AACAdditionPro.getInstance(),
                () -> {
                    // Put all users in a List for fast removal.
                    final List<User> users = new ArrayList<>(UserManager.getUsersUnwrapped());

                    // Iterate through all player-constellations
                    User observingUser;
                    while (!users.isEmpty())
                    {
                        /*
                            Remove the finished player to reduce the amount of added entries.
                            This makes sure the player won't have a connection with himself.
                            Remove index - 1 for the best performance.
                        */
                        observingUser = users.remove(users.size() - 1);

                        if (observingUser.getPlayer().getGameMode() != GameMode.SPECTATOR)
                        {
                            // All users can potentially be seen
                            for (final User watched : users)
                            {
                                // The watched player is also not in Spectator mode
                                if (watched.getPlayer().getGameMode() != GameMode.SPECTATOR &&
                                    // The players are in the same world
                                    observingUser.getPlayer().getWorld().getUID().equals(watched.getPlayer().getWorld().getUID()))
                                {
                                    playerConnections.addLast(new Pair(observingUser, watched));
                                }
                            }
                        }
                    }

                    Pair pair;
                    while (!playerConnections.isEmpty())
                    {
                        // Automatically empty the playerConnections
                        // Remove last entry for performance
                        pair = playerConnections.removeLast();

                        // The users are always in the same world (see abovel)
                        final double pairDistanceSquared = pair.usersOfPair[0].getPlayer().getLocation().distanceSquared(pair.usersOfPair[1].getPlayer().getLocation());

                        // Less than 1 block distance
                        // Everything (smaller than 1)^2 will result in something smaller than 1
                        if (pairDistanceSquared < 1)
                        {
                            updatePairHideMode(pair, HideMode.NONE);
                            return;
                        }

                        if (pairDistanceSquared > renderDistanceSquared)
                        {
                            updatePairHideMode(pair, hideAfterRenderDistance ?
                                                     HideMode.FULL :
                                                     HideMode.NONE);
                            return;
                        }

                        for (byte b = 0; b <= 1; b++)
                        {
                            final Player observer = pair.usersOfPair[b].getPlayer();
                            final Player watched = pair.usersOfPair[1 - b].getPlayer();

                            // ------------------------- Can one Player see the other ? ------------------------- //
                            boolean canSee;

                            // ------------------------------------- Glowing ------------------------------------ //
                            switch (ServerVersion.getActiveServerVersion())
                            {
                                case MC188:
                                    canSee = false;
                                    break;
                                case MC111:
                                case MC112:
                                case MC113:
                                    canSee = watched.hasPotionEffect(PotionEffectType.GLOWING);
                                    break;
                                default:
                                    throw new IllegalStateException("Unknown minecraft version");
                            }

                            // Not already able to see (due to e.g. glowing)
                            if (!canSee &&
                                // Not bypassed
                                !pair.usersOfPair[b].isBypassed() &&
                                // Has not logged in recently to prevent bugs
                                !pair.usersOfPair[b].getLoginData().recentlyUpdated(0, 3000))
                            {
                                //canSee = observer.hasLineOfSight(watched);
                                final Vector[] cameraVectors = getCameraVectors(observer);

                                final Hitbox hitboxOfWatched = watched.isSneaking() ?
                                                               Hitbox.ESP_SNEAKING_PLAYER :
                                                               Hitbox.ESP_PLAYER;

                                final AxisAlignedBB axisAlignedBB = hitboxOfWatched.constructBoundingBox(watched.getLocation());

                                // Try to see all camera vectors
                                for (Vector cameraVector : cameraVectors)
                                {
                                    // Every camera position will at most use 3 pyramids.
                                    final ParallelBasePyramidRectangle[] pyramids = new ParallelBasePyramidRectangle[3];

                                    // Get the correct sides:
                                    // X
                                    if (cameraVector.getX() < axisAlignedBB.getMinX())
                                    {
                                        pyramids[0] = new ParallelBasePyramidRectangle(
                                                cameraVector,
                                                new Vector(axisAlignedBB.getMinX(), axisAlignedBB.getMinY(), axisAlignedBB.getMinZ()),
                                                new Vector(axisAlignedBB.getMinX(), axisAlignedBB.getMinY(), axisAlignedBB.getMaxZ()),
                                                new Vector(axisAlignedBB.getMinX(), axisAlignedBB.getMaxY(), axisAlignedBB.getMinZ()),
                                                new Vector(axisAlignedBB.getMinX(), axisAlignedBB.getMaxY(), axisAlignedBB.getMaxZ())
                                        );
                                    }
                                    else if (cameraVector.getX() > axisAlignedBB.getMaxX())
                                    {
                                        pyramids[0] = new ParallelBasePyramidRectangle(
                                                cameraVector,
                                                new Vector(axisAlignedBB.getMaxX(), axisAlignedBB.getMinY(), axisAlignedBB.getMinZ()),
                                                new Vector(axisAlignedBB.getMaxX(), axisAlignedBB.getMinY(), axisAlignedBB.getMaxZ()),
                                                new Vector(axisAlignedBB.getMaxX(), axisAlignedBB.getMaxY(), axisAlignedBB.getMinZ()),
                                                new Vector(axisAlignedBB.getMaxX(), axisAlignedBB.getMaxY(), axisAlignedBB.getMaxZ())
                                        );
                                    }
                                    // If in between: No pyramid needed.

                                    // Y
                                    if (cameraVector.getY() < axisAlignedBB.getMinY())
                                    {
                                        pyramids[1] = new ParallelBasePyramidRectangle(
                                                cameraVector,
                                                new Vector(axisAlignedBB.getMinX(), axisAlignedBB.getMinY(), axisAlignedBB.getMinZ()),
                                                new Vector(axisAlignedBB.getMinX(), axisAlignedBB.getMinY(), axisAlignedBB.getMaxZ()),
                                                new Vector(axisAlignedBB.getMaxX(), axisAlignedBB.getMinY(), axisAlignedBB.getMinZ()),
                                                new Vector(axisAlignedBB.getMaxX(), axisAlignedBB.getMinY(), axisAlignedBB.getMaxZ())
                                        );
                                    }
                                    else if (cameraVector.getY() > axisAlignedBB.getMaxY())
                                    {
                                        pyramids[1] = new ParallelBasePyramidRectangle(
                                                cameraVector,
                                                new Vector(axisAlignedBB.getMinX(), axisAlignedBB.getMaxY(), axisAlignedBB.getMinZ()),
                                                new Vector(axisAlignedBB.getMinX(), axisAlignedBB.getMaxY(), axisAlignedBB.getMaxZ()),
                                                new Vector(axisAlignedBB.getMaxX(), axisAlignedBB.getMaxY(), axisAlignedBB.getMinZ()),
                                                new Vector(axisAlignedBB.getMaxX(), axisAlignedBB.getMaxY(), axisAlignedBB.getMaxZ())
                                        );
                                    }
                                    // If in between: No pyramid needed.

                                    // Z
                                    if (cameraVector.getZ() < axisAlignedBB.getMinZ())
                                    {
                                        pyramids[2] = new ParallelBasePyramidRectangle(
                                                cameraVector,
                                                new Vector(axisAlignedBB.getMinX(), axisAlignedBB.getMinY(), axisAlignedBB.getMinZ()),
                                                new Vector(axisAlignedBB.getMinX(), axisAlignedBB.getMaxY(), axisAlignedBB.getMinZ()),
                                                new Vector(axisAlignedBB.getMaxX(), axisAlignedBB.getMinY(), axisAlignedBB.getMinZ()),
                                                new Vector(axisAlignedBB.getMaxX(), axisAlignedBB.getMaxY(), axisAlignedBB.getMinZ())
                                        );
                                    }
                                    else if (cameraVector.getZ() > axisAlignedBB.getMaxZ())
                                    {
                                        pyramids[2] = new ParallelBasePyramidRectangle(
                                                cameraVector,
                                                new Vector(axisAlignedBB.getMinX(), axisAlignedBB.getMinY(), axisAlignedBB.getMaxZ()),
                                                new Vector(axisAlignedBB.getMinX(), axisAlignedBB.getMaxY(), axisAlignedBB.getMaxZ()),
                                                new Vector(axisAlignedBB.getMaxX(), axisAlignedBB.getMinY(), axisAlignedBB.getMaxZ()),
                                                new Vector(axisAlignedBB.getMaxX(), axisAlignedBB.getMaxY(), axisAlignedBB.getMaxZ())
                                        );
                                    }
                                    // If in between: No pyramid needed.

                                    // Now check all pyramids
                                    for (ParallelBasePyramidRectangle pyramid : pyramids)
                                    {
                                        for (Block block : pyramid.getBlocksInside(watched.getWorld()))
                                        {
                                            if (BlockUtils.isReallyOccluding(block.getType()))
                                            {

                                            }
                                        }
                                    }
                                }
                            }
                            else
                            {
                                canSee = true;
                            }

                            if (canSee)
                            {
                                // Can see the other player
                                updateHideMode(pair.usersOfPair[b], pair.usersOfPair[1 - b].getPlayer(), HideMode.NONE);
                            }
                            else
                            {
                                // Cannot see the other player
                                updateHideMode(pair.usersOfPair[b], pair.usersOfPair[1 - b].getPlayer(),
                                               // If the observed player is sneaking hide him fully
                                               pair.usersOfPair[1 - b].getPlayer().isSneaking() ?
                                               HideMode.FULL :
                                               HideMode.INFORMATION_ONLY);
                            }
                        }
                        // No special HideMode here as of the players being in 2 different worlds to decrease CPU load.
                    }

                    // Update_Ticks: the refresh-rate of the check.
                }, 0L, update_ticks);
    }

    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent event)
    {
        if (event.getNewGameMode() == GameMode.SPECTATOR)
        {
            final User user = UserManager.getUser(event.getPlayer().getUniqueId());

            // Not bypassed
            if (User.isUserInvalid(user))
            {
                return;
            }

            user.getEspInformationData().hiddenPlayers.keySet().forEach(hiddenPlayer -> updateHideMode(user, hiddenPlayer, HideMode.NONE));
        }
    }


    private static Vector[] getCameraVectors(final Player player)
    {
        /*
            All the vectors
            [0] = normal (eyeposition vector)
            [1] = front
            [2] = behind
        */
        final Vector[] vectors = new Vector[3];

        // Front vector : The 3rd person perspective in front of the player
        // Use THIRD_PERSON_OFFSET to get the maximum positions
        vectors[1] = player.getLocation().getDirection().clone().normalize().multiply(THIRD_PERSON_OFFSET);

        // Behind vector : The 3rd person perspective behind the player
        vectors[2] = vectors[1].clone().multiply(-1);

        final Location eyeLocation = player.getEyeLocation();

        // Normal
        vectors[0] = eyeLocation.toVector();

        // Do the Cameras intersect with Blocks
        // Get the length of the first intersection or 0 if there is none
        final double frontIntersection = VectorUtils.getDistanceToFirstIntersectionWithBlock(eyeLocation, vectors[1]);
        final double behindIntersection = VectorUtils.getDistanceToFirstIntersectionWithBlock(eyeLocation, vectors[2]);

        // There is an intersection in the front-vector
        if (frontIntersection != 0)
        {
            vectors[1].normalize().multiply(frontIntersection);
        }

        // There is an intersection in the behind-vector
        if (behindIntersection != 0)
        {
            vectors[2].normalize().multiply(behindIntersection);
        }

        return vectors;
    }

    private void updatePairHideMode(final Pair pair, final HideMode hideMode)
    {
        updateHideMode(pair.usersOfPair[0], pair.usersOfPair[1].getPlayer(), hideMode);
        updateHideMode(pair.usersOfPair[1], pair.usersOfPair[0].getPlayer(), hideMode);
    }

    private void updateHideMode(final User observer, final Player object, final HideMode hideMode)
    {
        if (observer.getEspInformationData().hiddenPlayers.get(object) != hideMode)
        {
            switch (hideMode)
            {
                case FULL:
                    observer.getEspInformationData().hiddenPlayers.put(object, HideMode.FULL);
                    // FULL: fullHider active, informationOnlyHider inactive
                    informationOnlyHider.unModifyInformation(observer.getPlayer(), object);
                    fullHider.modifyInformation(observer.getPlayer(), object);
                    break;
                case INFORMATION_ONLY:
                    observer.getEspInformationData().hiddenPlayers.put(object, HideMode.INFORMATION_ONLY);

                    // INFORMATION_ONLY: fullHider inactive, informationOnlyHider active
                    informationOnlyHider.modifyInformation(observer.getPlayer(), object);
                    fullHider.unModifyInformation(observer.getPlayer(), object);
                    break;
                case NONE:
                    observer.getEspInformationData().hiddenPlayers.remove(object);

                    // NONE: fullHider inactive, informationOnlyHider inactive
                    informationOnlyHider.unModifyInformation(observer.getPlayer(), object);
                    fullHider.unModifyInformation(observer.getPlayer(), object);
                    break;
            }
        }
    }

    @Override
    public void disable()
    {
        Bukkit.getScheduler().cancelTask(taskNumber);
    }

    private static class Pair
    {
        final User[] usersOfPair;

        Pair(final User a, final User b)
        {
            usersOfPair = new User[]{
                    a,
                    b
            };
        }

        @Override
        public boolean equals(final Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (o == null || getClass() != o.getClass())
            {
                return false;
            }

            // The other object
            final Pair pair = (Pair) o;
            return (usersOfPair[0].getPlayer().getUniqueId().equals(pair.usersOfPair[0].getPlayer().getUniqueId()) || usersOfPair[0].getPlayer().getUniqueId().equals(pair.usersOfPair[1].getPlayer().getUniqueId())) &&
                   (usersOfPair[1].getPlayer().getUniqueId().equals(pair.usersOfPair[1].getPlayer().getUniqueId()) || usersOfPair[1].getPlayer().getUniqueId().equals(pair.usersOfPair[0].getPlayer().getUniqueId())
                   );
        }

        @Override
        public int hashCode()
        {
            return usersOfPair[0].getPlayer().getUniqueId().hashCode() + usersOfPair[1].getPlayer().getUniqueId().hashCode();
        }
    }
}