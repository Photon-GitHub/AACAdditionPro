package de.photon.AACAdditionPro.checks.subchecks.clientcontrol;

import de.photon.AACAdditionPro.AdditionHackType;
import de.photon.AACAdditionPro.checks.ClientControlCheck;
import de.photon.AACAdditionPro.userdata.User;
import de.photon.AACAdditionPro.userdata.UserManager;
import de.photon.AACAdditionPro.util.files.ConfigUtils;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.List;

public class LiteloaderControl implements PluginMessageListener, ClientControlCheck
{
    private List<String> commandsOnDetection;

    private static final String[] LITELOADERFLAGS = {
            "LiteLoader"
    };

    // Plugin -> Client Channel for disabling mods: PERMISSIONSREPL
    // Client -> Plugin Channel: PERMISSIONSREPL

    @Override
    public void onPluginMessageReceived(final String channel, final Player player, final byte[] message)
    {
        final User user = UserManager.getUser(player.getUniqueId());

        if (user == null || user.isBypassed()) {
            return;
        }

        // Bypassed players are already filtered out.
        boolean flag = true;

        // MC-Brand for vanilla world-downloader
        final String brand = ClientControlCheck.getBrand(channel, message);

        if (brand != null) {
            flag = ClientControlCheck.stringContainsFlag(brand, LITELOADERFLAGS) || brand.contains("Lite");
        }

        // Should flag
        if (flag) {
            executeThresholds(user.getPlayer());
        }
    }

    @Override
    public List<String> getCommandsOnDetection()
    {
        return commandsOnDetection;
    }

    @Override
    public AdditionHackType getAdditionHackType()
    {
        return AdditionHackType.LITELOADER_CONTROL;
    }

    @Override
    public void subEnable()
    {
        commandsOnDetection = ConfigUtils.loadStringOrStringList(getAdditionHackType().getConfigString() + ".commands_on_detection");
    }

    @Override
    public String[] getPluginMessageChannels()
    {
        return new String[]{MCBRANDCHANNEL, "PERMISSIONSREPL"};
    }
}