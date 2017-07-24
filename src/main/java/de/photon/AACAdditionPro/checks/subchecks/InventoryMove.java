package de.photon.AACAdditionPro.checks.subchecks;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.AdditionHackType;
import de.photon.AACAdditionPro.checks.AACAdditionProCheck;
import de.photon.AACAdditionPro.userdata.User;
import de.photon.AACAdditionPro.userdata.UserManager;
import de.photon.AACAdditionPro.util.files.LoadFromConfiguration;
import de.photon.AACAdditionPro.util.mathematics.Hitbox;
import de.photon.AACAdditionPro.util.multiversion.ReflectionUtils;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerPosition;
import de.photon.AACAdditionPro.util.storage.management.ViolationLevelManagement;
import de.photon.AACAdditionPro.util.world.EntityUtils;
import me.konsolas.aac.api.AACAPIProvider;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.util.Vector;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;

public class InventoryMove extends PacketAdapter implements Listener, AACAdditionProCheck
{
    private final ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getAdditionHackType(), 100);

    @LoadFromConfiguration(configPath = ".cancel_vl")
    private int cancel_vl;
    @LoadFromConfiguration(configPath = ".min_tps")
    private double min_tps;

    private Field motXField;
    private Field motZField;
    private Method getHandle;

    public InventoryMove()
    {
        super(AACAdditionPro.getInstance(), ListenerPriority.LOWEST, PacketType.Play.Client.POSITION_LOOK, PacketType.Play.Client.POSITION);
    }

    @Override
    public void onPacketReceiving(final PacketEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        // Not bypassed
        if (user == null || user.isBypassed()) {
            return;
        }

        try {
            // Get motX and motZ
            final Object nmsHandle = this.getHandle.invoke(user.getPlayer());
            final double motX = this.motXField.getDouble(nmsHandle);
            final double motZ = this.motZField.getDouble(nmsHandle);

            final Vector input = new Vector(event.getPacket().getDoubles().readSafely(0),
                                            event.getPacket().getDoubles().readSafely(1),
                                            event.getPacket().getDoubles().readSafely(2)
            );

            final Vector knownPosition = user.getPlayer().getLocation().toVector();

            final double diffXYZ = Math.abs(input.getX() - knownPosition.getX()) + Math.abs(input.getY() - knownPosition.getY()) + Math.abs(input.getZ() - knownPosition.getZ());

            // No need to remove the Player himself as he is not added here

            // Check if this is a client side movement
            if (diffXYZ > 0.0D && motX != 0 && motZ != 0 &&
                // Not inside a vehicle
                !user.getPlayer().isInsideVehicle() &&
                // Not flying (may trigger some fps)
                !user.getPlayer().isFlying() &&
                // Not using an Elytra
                user.getElytraData().isNotFlyingWithElytra() &&
                // Player is in an inventory
                user.getInventoryData().hasOpenInventory() &&
                // Open inventory while jumping is covered by the safe-time and the fall distance
                // This covers the big jumps
                user.getPlayer().getNoDamageTicks() == 0 &&
                // Auto-Disable if TPS are too low
                AACAPIProvider.getAPI().getTPS() > min_tps)
            {
                final boolean currentlyNotJumping = (user.getPlayer().getVelocity().getY() <= 0 && user.getPlayer().getFallDistance() == 0);

                // Not allowed to start another jump in the inventory
                if (currentlyNotJumping) {
                    user.getPositionData().allowedToJump = false;
                }

                // If the player is jumping and is allowed to jump the max. time is 500 (478.5 is the legit jump time regarding the Tower check).
                // Otherwise it is 100 (little compensation for the "breaking" when sprinting previously
                final int allowedRecentlyOpenedTime = currentlyNotJumping ?
                                                      100 :
                                                      user.getPositionData().allowedToJump ?
                                                      500 :
                                                      100;

                // Was already in inventory or no air - movement (fall distance + velocity)
                if (user.getInventoryData().notRecentlyOpened(allowedRecentlyOpenedTime)) {

                    // Do the entity pushing stuff here (performance impact)
                    // No nearby entities that could push the player
                    final List<LivingEntity> nearbyPlayers = EntityUtils.getLivingEntitiesAroundPlayer(
                            user.getPlayer(),
                            // No division by 2 here as the hitbox of the other player is also important (-> 2 players)
                            Hitbox.PLAYER.getOffset() + 0.1,
                            Hitbox.PLAYER.getHeight() + 0.1,
                            Hitbox.PLAYER.getOffset() + 0.1);

                    if (nearbyPlayers.isEmpty()) {
                        vlManager.flag(user.getPlayer(), cancel_vl, () ->
                        {
                            event.getPacket().getDoubles().writeSafely(0, knownPosition.getX());
                            event.getPacket().getDoubles().writeSafely(2, knownPosition.getZ());

                            // Update client
                            final WrapperPlayServerPosition packet = new WrapperPlayServerPosition();

                            //Init with the known values
                            packet.setX(knownPosition.getX());
                            packet.setY(knownPosition.getY());
                            packet.setZ(knownPosition.getZ());
                            packet.setYaw(user.getPlayer().getLocation().getYaw());
                            packet.setPitch(user.getPlayer().getLocation().getPitch());

                            //Set the flags and send the packet
                            packet.setFlags(new HashSet<>());
                            packet.sendPacket(event.getPlayer());
                        }, () -> {});
                    }
                }
            } else {
                user.getPositionData().allowedToJump = true;
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void on(final InventoryClickEvent event)
    {
        final User user = UserManager.getUser(event.getWhoClicked().getUniqueId());

        // Not bypassed
        if (user == null || user.isBypassed()) {
            return;
        }
        // Flight may trigger this
        if (!user.getPlayer().getAllowFlight() &&
            // Not using an Elytra
            user.getElytraData().isNotFlyingWithElytra() &&
            // Sprinting and Sneaking as detection
            (user.getPlayer().isSprinting() || user.getPlayer().isSneaking()) &&
            // The player has an opened inventory
            user.getInventoryData().hasOpenInventory() &&
            // The player opened the inventory at least a quarter second ago
            user.getInventoryData().notRecentlyOpened(250) &&
            // Is the player moving
            user.getPositionData().hasPlayerMovedRecently(true))
        {
            vlManager.flag(user.getPlayer(), 4, cancel_vl, () ->
            {
                event.setCancelled(true);
                Bukkit.getScheduler().scheduleSyncDelayedTask(AACAdditionPro.getInstance(), () -> user.getPlayer().updateInventory(), 1L);
            }, () -> {});
        }
    }

    @Override
    public ViolationLevelManagement getViolationLevelManagement()
    {
        return vlManager;
    }

    @Override
    public AdditionHackType getAdditionHackType()
    {
        return AdditionHackType.INVENTORY_MOVE;
    }

    @Override
    public void subEnable()
    {
        // Reflection stuff
        try {
            //Get the String representing the version, e.g. v1_11_R1
            final String version = ReflectionUtils.getVersionString();

            // Load version class and reflect getHandle() out of it
            this.getHandle = ReflectionUtils.getMethodFromPath("org.bukkit.craftbukkit." + version + ".entity.CraftPlayer", "getHandle", true);

            // Get the motX and motZ fields
            final Class entityPlayerClazz = ReflectionUtils.loadClassFromPath("net.minecraft.server." + version + ".Entity");

            //Fields
            this.motXField = entityPlayerClazz.getDeclaredField("motX");
            this.motZField = entityPlayerClazz.getDeclaredField("motZ");
            this.motZField.setAccessible(true);
            this.motXField.setAccessible(true);
        } catch (ArrayIndexOutOfBoundsException | NoSuchMethodException | ClassNotFoundException | NoSuchFieldException e) {
            e.printStackTrace();
        }
    }
}