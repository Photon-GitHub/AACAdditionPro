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
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayClientLook;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayClientPositionLook;
import de.photon.AACAdditionPro.util.storage.management.ViolationLevelManagement;
import de.photon.AACAdditionPro.util.verbose.VerboseSender;

public class EqualRotation extends PacketAdapter implements AACAdditionProCheck
{
    private final ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getAdditionHackType(), 100);

    private int cancel_vl;
    private int timeout;

    public EqualRotation()
    {
        super(AACAdditionPro.getInstance(), ListenerPriority.LOW, PacketType.Play.Client.POSITION_LOOK, PacketType.Play.Client.LOOK);
    }

    @Override
    public void onPacketReceiving(final PacketEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        // Not bypassed
        if (user == null || user.isBypassed()) {
            return;
        }

        if (user.getLookPacketData().recentlyUpdated(timeout)) {
            event.setCancelled(true);
            user.getLookPacketData().lastYaw = Float.MIN_VALUE;
            user.getLookPacketData().lastPitch = Float.MIN_VALUE;
            return;
        }

        final float currentYaw, currentPitch;

        // Differentiate the packets
        if (event.getPacketType() == PacketType.Play.Client.POSITION_LOOK) {
            // PositionLook wrapper
            final WrapperPlayClientPositionLook positionLookWrapper = new WrapperPlayClientPositionLook(event.getPacket());
            currentYaw = positionLookWrapper.getYaw();
            currentPitch = positionLookWrapper.getPitch();

        } else if (event.getPacketType() == PacketType.Play.Client.LOOK) {
            // Look wrapper
            final WrapperPlayClientLook lookWrapper = new WrapperPlayClientLook(event.getPacket());
            currentYaw = lookWrapper.getYaw();
            currentPitch = lookWrapper.getPitch();

        } else {
            // Invalid data
            currentYaw = 0;
            currentPitch = 0;
            VerboseSender.sendVerboseMessage("EqualRotation: received invalid packet: " + event.getPacketType().toString(), true, true);
        }

        // Boat false positive (usually worse cheats in vehicles as well)
        if (!user.getPlayer().isInsideVehicle() &&
            // Not recently teleported
            !user.getTeleportData().recentlyUpdated(5000) &&
            // Same rotation values
            currentYaw == user.getLookPacketData().lastYaw &&
            currentPitch == user.getLookPacketData().lastPitch &&
            // Labymod fp when standing still
            user.getPositionData().hasPlayerMovedRecently(100, true))
        {
            vlManager.flag(user.getPlayer(), cancel_vl, () ->
            {
                event.setCancelled(true);
                user.getLookPacketData().updateTimeStamp();
            }, () -> {});
        } else {
            user.getLookPacketData().lastYaw = currentYaw;
            user.getLookPacketData().lastPitch = currentPitch;
        }
    }

    @Override
    public ViolationLevelManagement getViolationLevelManagement()
    {
        return vlManager;
    }

    @Override
    public void subEnable()
    {
        this.cancel_vl = AACAdditionPro.getInstance().getConfig().getInt(this.getAdditionHackType().getConfigString() + ".cancel_vl");
        this.timeout = AACAdditionPro.getInstance().getConfig().getInt(this.getAdditionHackType().getConfigString() + ".timeout");
    }

    @Override
    public AdditionHackType getAdditionHackType()
    {
        return AdditionHackType.EQUAL_ROTATION;
    }
}
