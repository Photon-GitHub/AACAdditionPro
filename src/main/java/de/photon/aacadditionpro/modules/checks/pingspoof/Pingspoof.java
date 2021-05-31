package de.photon.aacadditionpro.modules.checks.pingspoof;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketEvent;
import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.modules.Module;
import de.photon.aacadditionpro.modules.ModuleLoader;
import de.photon.aacadditionpro.modules.ModulePacketAdapter;
import de.photon.aacadditionpro.modules.ViolationModule;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.data.TimestampKey;
import de.photon.aacadditionpro.util.config.LoadFromConfiguration;
import de.photon.aacadditionpro.util.mathematics.Polynomial;
import de.photon.aacadditionpro.util.messaging.DebugSender;
import de.photon.aacadditionpro.util.packetwrappers.sentbyserver.WrapperPlayServerTransaction;
import de.photon.aacadditionpro.util.server.PingProvider;
import de.photon.aacadditionpro.util.violationlevels.Flag;
import de.photon.aacadditionpro.util.violationlevels.ViolationLevelManagement;
import de.photon.aacadditionpro.util.violationlevels.ViolationManagement;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitTask;

public class Pingspoof extends ViolationModule implements Listener
{
    private static final Polynomial VL_CALCULATOR_BELOW_500 = new Polynomial(-8.92857E-6, 0.0361786, 0.607143);
    private static final Polynomial VL_CALCULATOR_ABOVE_500 = new Polynomial(6.86217E-11, -1.26749E-6, 0.00802375, 12.8948);
    private BukkitTask pingSpoofTask;

    @LoadFromConfiguration(configPath = ".ping_leniency")
    private int pingLeniency;
    @LoadFromConfiguration(configPath = ".interval")
    private int interval;

    public Pingspoof()
    {
        super("Pingspoof");
    }

    @Override
    public void enable()
    {
        // Seconds -> Ticks
        val tickInterval = interval * 20;

        pingSpoofTask = Bukkit.getScheduler().runTaskTimerAsynchronously(AACAdditionPro.getInstance(), () -> {
            long serverPing;
            long echoPing;
            User user;

            // Create packet.
            val packet = new WrapperPlayServerTransaction();
            packet.setAccepted(false);
            packet.setActionNumber((short) 0);
            packet.setWindowId(0);

            for (Player player : Bukkit.getOnlinePlayers()) {
                user = User.getUser(player.getUniqueId());
                if (User.isUserInvalid(user, this)) continue;

                serverPing = PingProvider.getPing(player);
                echoPing = PingProvider.getEchoPing(user);

                // The player has not sent the received packet.
                if (echoPing < 0) {
                    DebugSender.getInstance().sendDebug("Player " + player + " tried to bypass pingspoof check.");
                    this.getManagement().flag(Flag.of(player).setAddedVl(35));
                } else {
                    val difference = Math.abs(serverPing - echoPing);

                    if (difference > pingLeniency) {
                        DebugSender.getInstance().sendDebug("Player " + player + " tried to spoof ping. Spoofed: " + serverPing + " | Actual: " + echoPing);
                        this.getManagement().flag(Flag.of(player).setAddedVl(difference > 500 ?
                                                                             VL_CALCULATOR_ABOVE_500.apply(difference).intValue() :
                                                                             VL_CALCULATOR_BELOW_500.apply(difference).intValue()));
                    }
                }

                // Send the new packet.
                user.getTimestampMap().at(TimestampKey.PINGSPOOF_RECEIVED_PACKET).setToZero();
                packet.sendPacket(player);
                user.getTimestampMap().at(TimestampKey.PINGSPOOF_SENT_PACKET).update();
            }
        }, 600, tickInterval);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        val user = User.getUser(event.getPlayer().getUniqueId());
        if (User.isUserInvalid(user, this)) return;

        // Update the received once to make sure the player is not initially flagged for not sending a received packet.
        user.getTimestampMap().at(TimestampKey.PINGSPOOF_RECEIVED_PACKET).update();
    }

    @Override
    public void disable()
    {
        pingSpoofTask.cancel();
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        val adapter = new PingspoofPacketAdapter(this);
        return ModuleLoader.builder(this)
                           .addPacketListeners(adapter)
                           .build();
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return new ViolationLevelManagement(this, 300L);
    }

    private static class PingspoofPacketAdapter extends ModulePacketAdapter
    {
        public PingspoofPacketAdapter(Module module)
        {
            super(module, ListenerPriority.HIGH, PacketType.Play.Client.TRANSACTION);
        }

        @Override
        public void onPacketReceiving(PacketEvent event)
        {
            val user = User.safeGetUserFromPacketEvent(event);
            if (User.isUserInvalid(user, this.getModule())) return;

            // We have now received the answer.
            user.getTimestampMap().at(TimestampKey.PINGSPOOF_RECEIVED_PACKET).update();
        }
    }
}
