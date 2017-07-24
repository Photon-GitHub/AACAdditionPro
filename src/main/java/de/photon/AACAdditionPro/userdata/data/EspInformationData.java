package de.photon.AACAdditionPro.userdata.data;

import de.photon.AACAdditionPro.userdata.Data;
import de.photon.AACAdditionPro.userdata.User;
import de.photon.AACAdditionPro.util.visibility.HideMode;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EspInformationData extends Data
{
    public final Map<UUID, HideMode> hiddenPlayers = new HashMap<>();

    public EspInformationData(final User theUser)
    {
        super(true, theUser);
    }

    @EventHandler
    @Override
    public void on(final PlayerQuitEvent event)
    {
        hiddenPlayers.remove(event.getPlayer().getUniqueId());
        // Listener cleanup
        super.on(event);
    }
}