package de.photon.AACAdditionPro.modules.checks;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.modules.ListenerModule;
import de.photon.AACAdditionPro.modules.ModuleType;
import de.photon.AACAdditionPro.modules.PacketListenerModule;
import de.photon.AACAdditionPro.modules.ViolationModule;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.user.UserManager;
import de.photon.AACAdditionPro.user.data.PositionData;
import de.photon.AACAdditionPro.util.entity.EntityUtil;
import de.photon.AACAdditionPro.util.files.configs.LoadFromConfiguration;
import de.photon.AACAdditionPro.util.inventory.InventoryUtils;
import de.photon.AACAdditionPro.util.mathematics.Hitbox;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerPosition;
import de.photon.AACAdditionPro.util.reflection.Reflect;
import de.photon.AACAdditionPro.util.violationlevels.ViolationLevelManagement;
import de.photon.AACAdditionPro.util.world.ChunkUtils;
import me.konsolas.aac.api.AACAPIProvider;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.util.Vector;

public class InventoryMove extends PacketAdapter implements ListenerModule, PacketListenerModule, ViolationModule
{
    private final ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getModuleType(), 100);

    @LoadFromConfiguration(configPath = ".cancel_vl")
    private int cancel_vl;
    @LoadFromConfiguration(configPath = ".min_tps")
    private double min_tps;
    @LoadFromConfiguration(configPath = ".lenience_millis")
    private int lenience_millis;

    public InventoryMove()
    {
        super(AACAdditionPro.getInstance(), ListenerPriority.LOWEST, PacketType.Play.Client.POSITION_LOOK, PacketType.Play.Client.POSITION);
    }

    @Override
    public void onPacketReceiving(final PacketEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        // Not bypassed
        if (User.isUserInvalid(user, this.getModuleType()))
        {
            return;
        }

        // Get motX and motZ
        final Object nmsHandle = Reflect.fromOBC("entity.CraftPlayer").method("getHandle").invoke(user.getPlayer());

        // motX and motZ are not 0 if the player is collided accordingly.
        final double motX = Reflect.fromNMS("Entity").field("motX").from(nmsHandle).asDouble();
        final double motZ = Reflect.fromNMS("Entity").field("motZ").from(nmsHandle).asDouble();

        final Vector moveTo = new Vector(event.getPacket().getDoubles().readSafely(0),
                                         event.getPacket().getDoubles().readSafely(1),
                                         event.getPacket().getDoubles().readSafely(2)
        );

        final Location knownPosition = user.getPlayer().getLocation();

        // Check if this is a clientside movement:
        // Position Vectors are not the same
        if (!moveTo.equals(knownPosition.toVector()) &&
            // or movement is not 0
            motX != 0 &&
            motZ != 0 &&
            // Not inside a vehicle
            !user.getPlayer().isInsideVehicle() &&
            // Not flying (may trigger some fps)
            !user.getPlayer().isFlying() &&
            // Make sure the current chunk of the player is loaded so the liquids method does not cause async entity
            // world add errors.
            ChunkUtils.isChunkLoaded(user.getPlayer().getLocation()) &&
            // The player is currently not in a liquid (liquids push)
            !EntityUtil.isHitboxInLiquids(knownPosition, user.getPlayer().isSneaking() ?
                                                         Hitbox.SNEAKING_PLAYER :
                                                         Hitbox.PLAYER) &&
            // Not using an Elytra
            !EntityUtil.isFlyingWithElytra(user.getPlayer()) &&
            // Player is in an inventory
            user.getInventoryData().hasOpenInventory() &&
            // Player has not been hit recently
            user.getPlayer().getNoDamageTicks() == 0 &&
            // Auto-Disable if TPS are too low
            AACAPIProvider.getAPI().getTPS() > min_tps)
        {
            // Open inventory while jumping is covered by the safe-time and the fall distance
            // This covers the big jumps
            final boolean currentlyNotJumping = (user.getPlayer().getVelocity().getY() <= 0 && user.getPlayer().getFallDistance() == 0);

            // Not allowed to start another jump in the inventory
            if (currentlyNotJumping)
            {
                user.getPositionData().allowedToJump = false;
            }

            // If the player is jumping and is allowed to jump the max. time is 500 (478.5 is the legit jump time regarding the Tower check).
            // Otherwise it is 100 (little compensation for the "breaking" when sprinting previously
            final int allowedRecentlyOpenedTime = (currentlyNotJumping ?
                                                   100 :
                                                   user.getPositionData().allowedToJump ?
                                                   500 :
                                                   100) + lenience_millis;

            // Was already in inventory or no air - movement (fall distance + velocity)
            if (user.getInventoryData().notRecentlyOpened(allowedRecentlyOpenedTime) &&
                // Do the entity pushing stuff here (performance impact)
                // No nearby entities that could push the player
                EntityUtil.getLivingEntitiesAroundPlayer(
                        user.getPlayer(),
                        // No division by 2 here as the hitbox of the other player is also important (-> 2 players)
                        Hitbox.PLAYER.getOffsetX() + 0.1,
                        Hitbox.PLAYER.getHeight() + 0.1,
                        Hitbox.PLAYER.getOffsetZ() + 0.1).isEmpty())
            {
                vlManager.flag(user.getPlayer(), cancel_vl, () ->
                {
                    //TODO: TEST THIS; THIS MIGHT SEND EMPTY PACKETS ?
                    event.setCancelled(true);
                    //event.getPacket().getDoubles().writeSafely(0, knownPosition.getX());
                    //event.getPacket().getDoubles().writeSafely(2, knownPosition.getZ());

                    // Update client
                    final WrapperPlayServerPosition packet = new WrapperPlayServerPosition();

                    //Init with the known values
                    packet.setX(knownPosition.getX());
                    packet.setY(knownPosition.getY());
                    packet.setZ(knownPosition.getZ());
                    packet.setYaw(knownPosition.getYaw());
                    packet.setPitch(knownPosition.getPitch());

                    //Set the flags and send the packet
                    packet.setNoFlags();
                    packet.sendPacket(event.getPlayer());
                }, () -> {});
            }
        }
        else
        {
            user.getPositionData().allowedToJump = true;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void on(final InventoryClickEvent event)
    {
        final User user = UserManager.getUser(event.getWhoClicked().getUniqueId());

        // Not bypassed
        if (User.isUserInvalid(user, this.getModuleType()))
        {
            return;
        }

        // Flight may trigger this
        if (!user.getPlayer().getAllowFlight() &&
            // Not using an Elytra
            !EntityUtil.isFlyingWithElytra(user.getPlayer()) &&
            // Sprinting and Sneaking as detection
            (user.getPlayer().isSprinting() || user.getPlayer().isSneaking()) &&
            // The player has an opened inventory
            user.getInventoryData().hasOpenInventory() &&
            // The player opened the inventory at least a quarter second ago
            user.getInventoryData().notRecentlyOpened(250) &&
            // Is the player moving
            user.getPositionData().hasPlayerMovedRecently(1000, PositionData.MovementType.ANY))
        {
            vlManager.flag(user.getPlayer(), 4, cancel_vl, () ->
            {
                event.setCancelled(true);
                InventoryUtils.syncUpdateInventory(user.getPlayer());
            }, () -> {});
        }
    }

    @Override
    public ViolationLevelManagement getViolationLevelManagement()
    {
        return vlManager;
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.INVENTORY_MOVE;
    }

}
