package de.photon.AACAdditionPro.util.entities;

import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.util.entities.displayinformation.DisplayInformation;
import de.photon.AACAdditionPro.util.entities.equipment.EntityEquipmentDatabase;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerNamedEntitySpawn;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerPlayerInfo;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;

public class ClientsidePlayerEntity extends ClientsideEntity
{
    @Getter
    private final WrappedGameProfile gameProfile;
    @Getter
    @Setter
    private int ping;

    @Getter
    @Setter
    private Team currentTeam;

    private int task;

    public ClientsidePlayerEntity(final Player observedPlayer, final WrappedGameProfile gameProfile, final double entityOffset, final double offsetRandomizationRange, double minXZDifference)
    {
        super(observedPlayer);
        // Get skin data and name
        this.gameProfile = gameProfile;

        task = Bukkit.getScheduler().scheduleSyncRepeatingTask(AACAdditionPro.getInstance(), () ->
        {
            // Teams + Scoreboard
            DisplayInformation.applyTeams(this);

            // Location
            final Location playerLocation = this.observedPlayer.getLocation();

            Location moveToLocation = playerLocation.clone();

            // Move behind the player to make the entity not disturb players
            // Important: the negative offset!
            moveToLocation.add(moveToLocation.getDirection().clone().normalize().multiply(-entityOffset + ThreadLocalRandom.current().nextDouble(offsetRandomizationRange)));

            final double currentXZDifference = Math.sqrt(Math.pow(moveToLocation.getX() - playerLocation.getX(), 2) + Math.pow(moveToLocation.getZ() - playerLocation.getZ(), 2));

            // To prevent willingly causing false positives and reducing the entities' visibility for legit players
            if (currentXZDifference < minXZDifference) {
                // Special case if both values are zero
                if (currentXZDifference == 0) {
                    final Location noPitchLocation = location.clone();
                    noPitchLocation.setPitch(0);

                    moveToLocation.add(noPitchLocation.getDirection().normalize().multiply(minXZDifference));
                } else {
                    // The y-value should be the same
                    final double preY = moveToLocation.getY();
                    moveToLocation.multiply(minXZDifference / currentXZDifference);
                    moveToLocation.setY(preY);
                }
            }

            this.move(moveToLocation);
        }, 0L, 1L);

        recursiveUpdatePing();
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
            DisplayInformation.updatePing(this);
            recursiveUpdatePing();
        }, 10 + ThreadLocalRandom.current().nextInt(35));
    }

    // ---------------------------------------------------------------- Spawn --------------------------------------------------------------- //

    @Override
    public void spawn(Location location)
    {
        super.spawn(location);
        this.lastLocation = location.clone();
        this.location = location.clone();
        // Add the player with PlayerInfo
        final PlayerInfoData playerInfoData = new PlayerInfoData(this.gameProfile, ping, EnumWrappers.NativeGameMode.SURVIVAL, null);

        final WrapperPlayServerPlayerInfo playerInfoWrapper = new WrapperPlayServerPlayerInfo();
        playerInfoWrapper.setAction(EnumWrappers.PlayerInfoAction.ADD_PLAYER);
        playerInfoWrapper.setData(Collections.singletonList(playerInfoData));

        playerInfoWrapper.sendPacket(observedPlayer);

        // DataWatcher
        final WrappedDataWatcher dataWatcher = new WrappedDataWatcher();
        dataWatcher.setObject(6, (float) 20);
        dataWatcher.setObject(10, (byte) 127); //TODO player's probably do have more things set here

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
        DisplayInformation.applyTeams(this);

        // Entity equipment + armor
        EntityEquipmentDatabase.getRandomEquipment(false).equipPlayerEntity(this);
    }

    // --------------------------------------------------------------- Despawn -------------------------------------------------------------- //

    @Override
    public void despawn()
    {
        super.despawn();
        // Cancel all tasks of this entity
        Bukkit.getScheduler().cancelTask(task);

        removeFromTab();
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
