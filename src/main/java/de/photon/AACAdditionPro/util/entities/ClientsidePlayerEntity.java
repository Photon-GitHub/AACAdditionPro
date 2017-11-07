package de.photon.AACAdditionPro.util.entities;

import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.ModuleType;
import de.photon.AACAdditionPro.util.entities.displayinformation.DisplayInformation;
import de.photon.AACAdditionPro.util.entities.equipment.Equipment;
import de.photon.AACAdditionPro.util.entities.equipment.category.WeaponsEquipmentCategory;
import de.photon.AACAdditionPro.util.entities.movement.submovements.BasicFollowMovement;
import de.photon.AACAdditionPro.util.mathematics.Hitbox;
import de.photon.AACAdditionPro.util.mathematics.MathUtils;
import de.photon.AACAdditionPro.util.multiversion.ServerVersion;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerEntityEquipment;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerNamedEntitySpawn;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerPlayerInfo;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;

public class ClientsidePlayerEntity extends ClientsideEntity
{
    private boolean shouldAssignTeam;
    private boolean shouldSwing;
    private boolean shouldSwap;

    @Getter
    private final WrappedGameProfile gameProfile;

    @Getter
    @Setter
    private int ping;

    @Getter
    @Setter(value = AccessLevel.PROTECTED)
    private Team currentTeam;

    // Main ticker for the entity
    private byte lastSwing = 0;
    private byte lastSwap = 0;

    private short lastJump = 0;

    private Equipment equipment;

    public ClientsidePlayerEntity(final Player observedPlayer, final WrappedGameProfile gameProfile, final double entityOffset, final double offsetRandomizationRange, double minXZDifference)
    {
        super(observedPlayer, Hitbox.PLAYER, new BasicFollowMovement(observedPlayer, entityOffset, offsetRandomizationRange, minXZDifference));

        // Get skin data and name
        this.gameProfile = gameProfile;

        // EquipmentData
        this.equipment = new Equipment(this);

        recursiveUpdatePing();

        // Init additional behaviour configs
        shouldAssignTeam = AACAdditionPro.getInstance().getConfig().getBoolean(ModuleType.KILLAURA_ENTITY.getConfigString() + ".behaviour.team.enabled");
        shouldSwing = AACAdditionPro.getInstance().getConfig().getBoolean(ModuleType.KILLAURA_ENTITY.getConfigString() + ".behaviour.swing.enabled");
        shouldSwap = AACAdditionPro.getInstance().getConfig().getBoolean(ModuleType.KILLAURA_ENTITY.getConfigString() + ".behaviour.swap.enabled");
    }

    @Override
    protected void tick()
    {
        super.tick();

        // Teams + Scoreboard
        if (shouldAssignTeam)
        {
            DisplayInformation.applyTeams(this);
        }

        // Try to look to the target
        Location target = this.observedPlayer.getLocation();
        double diffX = target.getX() - this.location.getX();
        double diffY = target.getY() + this.observedPlayer.getEyeHeight() * 0.9D - (this.location.getY() + this.observedPlayer.getEyeHeight());
        double diffZ = target.getZ() - this.location.getZ();
        double dist = Math.hypot(diffX, diffZ);

        float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
        float pitch = (float) Math.toDegrees(Math.asin(diffY / dist));

        pitch += ThreadLocalRandom.current().nextInt(5);

        pitch = reduceAngle(pitch, 90);

        this.headYaw = reduceAngle((float) MathUtils.randomBoundaryDouble(yaw - 10, 20), 180);

        this.location.setYaw(yaw);
        this.location.setPitch(pitch);
        this.move(this.location);

        // Maybe we should switch movement states?
        if (lastJump++ > MathUtils.randomBoundaryInt(30, 80))
        {
            lastJump = 0;
            jump();
        }

        // Swing items if enabled
        if (shouldSwing)
        {
            if (lastSwing++ > MathUtils.randomBoundaryInt(15, 35))
            {
                lastSwing = 0;

                if (isSwingable(equipment.getMainHand().getType()))
                {
                    swing();
                }
            }
        }

        // Swap items if needed
        if (shouldSwap)
        {
            if (lastSwap++ > MathUtils.randomBoundaryInt(40, 65))
            {
                lastSwap = 0;
                equipment.equipInHand();
                equipment.equipPlayerEntity();
            }
        }
    }

    // -------------------------------------------------------------- Yaw/Pitch ------------------------------------------------------------- //

    /**
     * Reduces the angle to make it fit the spectrum of -minMax til +minMax in steps of minMax
     *
     * @param input  the initial angle
     * @param minMax the boundary in the positive and negative spectrum. The parameter itself must be > 0.
     */
    private float reduceAngle(float input, float minMax)
    {
        while (Math.abs(input) > minMax)
        {
            input -= Math.signum(input) * minMax;
        }
        return input;
    }

    private boolean isSwingable(Material material)
    {
        WeaponsEquipmentCategory weaponsEquipmentCategory = new WeaponsEquipmentCategory();
        return weaponsEquipmentCategory.getMaterials().contains(material);
    }

    // --------------------------------------------------------------- General -------------------------------------------------------------- //

    public String getName()
    {
        return this.gameProfile.getName();
    }

    // -------------------------------------------------------------- Simulation ------------------------------------------------------------ //

    /**
     * This changes the Ping of the {@link ClientsidePlayerEntity}.
     * The recursive call is needed to randomize the ping-update
     */
    private void recursiveUpdatePing()
    {
        Bukkit.getScheduler().scheduleSyncDelayedTask(AACAdditionPro.getInstance(), () -> {
            if (this.isValid())
            {
                fakePingForObservedPlayer(MathUtils.randomBoundaryInt(21, 4));
                recursiveUpdatePing();
            }
        }, (long) MathUtils.randomBoundaryInt(10, 35));
    }

    /**
     * The real {@link java.lang.reflect.Method} that handles the ping-changing once the given {@link ClientsidePlayerEntity} is confirmed as valid.
     *
     * @param ping the new ping of the {@link ClientsidePlayerEntity}
     */
    private void fakePingForObservedPlayer(final int ping)
    {
        if (this.isSpawned())
        {
            final WrapperPlayServerPlayerInfo playerInfoWrapper = new WrapperPlayServerPlayerInfo();
            playerInfoWrapper.setAction(EnumWrappers.PlayerInfoAction.UPDATE_LATENCY);
            playerInfoWrapper.setData(Collections.singletonList(
                    // The new information of about the Entity.
                    new PlayerInfoData(this.getGameProfile(), ping, EnumWrappers.NativeGameMode.SURVIVAL, null)));
            playerInfoWrapper.sendPacket(this.observedPlayer);
        }
    }

    @Override
    public void setVisibility(final boolean visible)
    {
        super.setVisibility(visible);

        this.shouldSwap = visible;

        if (!visible)
        {
            WrapperPlayServerEntityEquipment.clearAllSlots(this.getEntityID(), this.observedPlayer);
        }
    }

    // ---------------------------------------------------------------- Teams --------------------------------------------------------------- //

    /**
     * Used to make the {@link ClientsidePlayerEntity} join a new {@link Team}
     * If the {@link ClientsidePlayerEntity} is already in a {@link Team} it will try to leave it first.
     *
     * @param team the new {@link Team} to join.
     */
    public void joinTeam(Team team) throws IllegalStateException
    {
        this.leaveTeam();
        team.addEntry(this.getGameProfile().getName());
        this.setCurrentTeam(team);
    }

    /**
     * Used to make the {@link ClientsidePlayerEntity} leave its current {@link Team}
     * If the {@link ClientsidePlayerEntity} is in no team nothing will happen.
     */
    public void leaveTeam() throws IllegalStateException
    {
        if (this.getCurrentTeam() != null)
        {
            this.getCurrentTeam().removeEntry(this.getGameProfile().getName());
            this.setCurrentTeam(null);
        }
    }

    // ---------------------------------------------------------------- Spawn --------------------------------------------------------------- //

    @Override
    public void spawn(Location location)
    {
        super.spawn(location);
        this.lastLocation = location.clone();
        this.move(location);
        // Add the player with PlayerInfo
        final PlayerInfoData playerInfoData = new PlayerInfoData(this.gameProfile, ping, EnumWrappers.NativeGameMode.SURVIVAL, null);

        final WrapperPlayServerPlayerInfo playerInfoWrapper = new WrapperPlayServerPlayerInfo();
        playerInfoWrapper.setAction(EnumWrappers.PlayerInfoAction.ADD_PLAYER);
        playerInfoWrapper.setData(Collections.singletonList(playerInfoData));

        playerInfoWrapper.sendPacket(observedPlayer);

        // DataWatcher
        final WrappedDataWatcher dataWatcher = new WrappedDataWatcher();

        switch (ServerVersion.getActiveServerVersion())
        {
            case MC188:
                dataWatcher.setObject(6, 20F);
                dataWatcher.setObject(10, (byte) 127);
                break;
            case MC110:
            case MC111:
            case MC112:
                WrappedDataWatcher.WrappedDataWatcherObject[] objects = new WrappedDataWatcher.WrappedDataWatcherObject[2];
                objects[0] = new WrappedDataWatcher.WrappedDataWatcherObject(6, WrappedDataWatcher.Registry.get(Byte.class));
                objects[1] = new WrappedDataWatcher.WrappedDataWatcherObject(10, WrappedDataWatcher.Registry.get(Byte.class));
                dataWatcher.setObject(objects[0], (byte) 20);
                dataWatcher.setObject(objects[1], (byte) 127);
                break;
            default:
                throw new IllegalStateException("Unknown minecraft version");
        }

        // Spawn the entity
        final WrapperPlayServerNamedEntitySpawn spawnEntityWrapper = new WrapperPlayServerNamedEntitySpawn();

        spawnEntityWrapper.setEntityID(this.entityID);
        spawnEntityWrapper.setMetadata(dataWatcher);
        spawnEntityWrapper.setPosition(location.toVector());
        spawnEntityWrapper.setPlayerUUID(this.gameProfile.getUUID());
        spawnEntityWrapper.setYaw(ThreadLocalRandom.current().nextInt(15));
        spawnEntityWrapper.setPitch(ThreadLocalRandom.current().nextInt(15));

        spawnEntityWrapper.sendPacket(observedPlayer);

        // Debug
        // System.out.println("Sent player spawn of bot " + this.entityID + " for " + observedPlayer.getName() + " @ " + location);

        // Set the team (most common on respawn)
        if (shouldAssignTeam)
        {
            DisplayInformation.applyTeams(this);
        }

        // Entity equipment + armor
        this.equipment.equipArmor();
        this.equipment.equipInHand();
        this.equipment.equipPlayerEntity();
    }

    // --------------------------------------------------------------- Despawn -------------------------------------------------------------- //

    @Override
    public void despawn()
    {
        if (isSpawned())
        {
            removeFromTab();
        }

        super.despawn();
    }

    private void removeFromTab()
    {
        // Remove the player with PlayerInfo
        final PlayerInfoData playerInfoData = new PlayerInfoData(this.gameProfile, 0, EnumWrappers.NativeGameMode.SURVIVAL, null);

        final WrapperPlayServerPlayerInfo playerInfoWrapper = new WrapperPlayServerPlayerInfo();
        playerInfoWrapper.setAction(EnumWrappers.PlayerInfoAction.REMOVE_PLAYER);
        playerInfoWrapper.setData(Collections.singletonList(playerInfoData));

        playerInfoWrapper.sendPacket(observedPlayer);
    }
}
