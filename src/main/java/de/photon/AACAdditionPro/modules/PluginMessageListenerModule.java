package de.photon.AACAdditionPro.modules;

import de.photon.AACAdditionPro.util.pluginmessage.MessageChannel;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.Collections;
import java.util.Set;

/**
 * Marks a {@link Module} which utilizes the {@link PluginMessageListener} functionality.
 */
public interface PluginMessageListenerModule extends Module, PluginMessageListener
{
    /**
     * Additional chores needed to enable a {@link PluginMessageListenerModule}
     */
    static void enable(final PluginMessageListenerModule module)
    {
        module.getIncomingChannels().forEach(messageChannel -> messageChannel.registerIncomingChannel(module));
        module.getOutgoingChannels().forEach(messageChannel -> messageChannel.registerOutgoingChannel(module));
    }

    /**
     * Additional chores needed to disable a {@link PluginMessageListenerModule}
     */
    static void disable(final PluginMessageListenerModule module)
    {
        module.getIncomingChannels().forEach(messageChannel -> messageChannel.unregisterIncomingChannel(module));
        module.getOutgoingChannels().forEach(messageChannel -> messageChannel.unregisterOutgoingChannel(module));
    }

    // Don't make this method necessary to override (e.g. for sending only modules)
    @Override
    default void onPluginMessageReceived(String s, Player player, byte[] bytes) {}

    /**
     * This returns the incoming channels the {@link PluginMessageListener} should listen to.
     */
    default Set<MessageChannel> getIncomingChannels()
    {
        return Collections.emptySet();
    }

    /**
     * This returns the outgoing channels the {@link PluginMessageListener} should listen to.
     */
    default Set<MessageChannel> getOutgoingChannels()
    {
        return Collections.emptySet();
    }
}
