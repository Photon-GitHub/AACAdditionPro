package de.photon.AACAdditionPro.checks.subchecks.clientcontrol;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.AdditionHackType;
import de.photon.AACAdditionPro.checks.ClientControlCheck;
import de.photon.AACAdditionPro.userdata.User;
import de.photon.AACAdditionPro.userdata.UserManager;
import de.photon.AACAdditionPro.util.files.ConfigUtils;
import de.photon.AACAdditionPro.util.files.LoadFromConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.List;

public class BetterSprintingControl implements PluginMessageListener, ClientControlCheck
{
    private List<String> commandsOnDetection;

    @LoadFromConfiguration(configPath = ".disable")
    private boolean disable;

    @Override
    public void onPluginMessageReceived(final String channel, final Player player, final byte[] message)
    {
        final User user = UserManager.getUser(player.getUniqueId());

        if (user == null || user.isBypassed()) {
            return;
        }

        // Bypassed players are already filtered out.
        // The mod provides a method to disable it
        if (disable) {
            final ByteArrayDataOutput out1 = ByteStreams.newDataOutput();
            out1.writeByte(1);

            // The channel is always BSM, the right one.
            user.getPlayer().sendPluginMessage(AACAdditionPro.getInstance(), channel, out1.toByteArray());
        }

        executeThresholds(user.getPlayer());
    }

    @Override
    public String[] getPluginMessageChannels()
    {
        return new String[]{"BSM"};
    }

    @Override
    public void subEnable()
    {
        commandsOnDetection = ConfigUtils.loadStringOrStringList(this.getAdditionHackType().getConfigString() + ".commands_on_detection");
    }

    @Override
    public List<String> getCommandsOnDetection()
    {
        return commandsOnDetection;
    }

    @Override
    public AdditionHackType getAdditionHackType()
    {
        return AdditionHackType.BETTERSPRINTING_CONTROL;
    }
}