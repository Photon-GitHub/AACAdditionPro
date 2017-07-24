package de.photon.AACAdditionPro.checks.subchecks;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.AdditionHackType;
import de.photon.AACAdditionPro.checks.AACAdditionProCheck;
import de.photon.AACAdditionPro.userdata.User;
import de.photon.AACAdditionPro.userdata.UserManager;
import de.photon.AACAdditionPro.util.files.LoadFromConfiguration;
import de.photon.AACAdditionPro.util.storage.management.ViolationLevelManagement;
import me.konsolas.aac.api.AACAPIProvider;
import org.bukkit.Bukkit;

public class Fastswitch extends PacketAdapter implements AACAdditionProCheck
{
    private final ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getAdditionHackType(), 120);

    @LoadFromConfiguration(configPath = ".cancel_vl")
    private int cancel_vl;
    @LoadFromConfiguration(configPath = "max_ping")
    private double max_ping;
    @LoadFromConfiguration(configPath = "switch_milliseconds")
    private int switch_milliseconds;

    public Fastswitch()
    {
        super(AACAdditionPro.getInstance(), PacketType.Play.Client.HELD_ITEM_SLOT);
    }

    @Override
    public void onPacketReceiving(final PacketEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        // Not bypassed
        if (user == null || user.isBypassed()) {
            return;
        }

        // Tps are high enough
        if (AACAPIProvider.getAPI().getTPS() > 19 &&
            event.getPacket().getBytes().readSafely(0) != null &&
            // Prevent the detection of scrolling
            !canBeLegit(user.getPlayer().getInventory().getHeldItemSlot(), event.getPacket().getBytes().readSafely(0)))
        {
            // Already switched in the given timeframe
            if (user.getFastSwitchData().recentlyUpdated(switch_milliseconds)) {

                // The ping is valid and in the borders that are set in the config
                if (max_ping < 0 || AACAPIProvider.getAPI().getPing(user.getPlayer()) < max_ping) {
                    vlManager.flag(user.getPlayer(),
                                   cancel_vl,
                                   () -> event.setCancelled(true),
                                   () -> Bukkit.getScheduler().scheduleSyncDelayedTask(AACAdditionPro.getInstance(), () -> user.getPlayer().updateInventory(), 1L));
                }
            }

            user.getFastSwitchData().updateTimeStamp();
        }
    }

    /**
     * Used to acknowledge if somebody can be legit.
     * I.e. that players can scroll very fast, but then the neighbor slot is always the one that gets called next.
     */
    private static boolean canBeLegit(final int currentSlot, final int newHeldItemSlot)
    {
        return (currentSlot == 8 || currentSlot == 0) && (newHeldItemSlot == 8 || newHeldItemSlot == 0) || !(newHeldItemSlot + 1 > currentSlot || newHeldItemSlot - 1 < currentSlot);
    }

    @Override
    public ViolationLevelManagement getViolationLevelManagement()
    {
        return vlManager;
    }

    @Override
    public AdditionHackType getAdditionHackType()
    {
        return AdditionHackType.FASTSWITCH;
    }
}