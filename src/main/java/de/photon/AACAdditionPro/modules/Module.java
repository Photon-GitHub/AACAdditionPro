package de.photon.AACAdditionPro.modules;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.util.VerboseSender;
import de.photon.AACAdditionPro.util.files.configs.ConfigUtils;

public interface Module
{
    /**
     * This enables the check according to its interfaces.
     */
    static void enableModule(final Module module)
    {
        try
        {
            // ServerVersion check
            if (module instanceof RestrictedServerVersionModule && RestrictedServerVersionModule.allowedToStart((RestrictedServerVersionModule) module))
            {
                VerboseSender.getInstance().sendVerboseMessage(module.getConfigString() + " is not compatible with your server version.");
                return;
            }

            // Enabled
            if (!AACAdditionPro.getInstance().getConfig().getBoolean(module.getConfigString() + ".enabled"))
            {
                VerboseSender.getInstance().sendVerboseMessage(module.getConfigString() + " was chosen not to be enabled.");
                return;
            }

            // Dependency check
            if (module instanceof DependencyModule && !DependencyModule.allowedToStart((DependencyModule) module))
            {
                VerboseSender.getInstance().sendVerboseMessage(module.getConfigString() + " has been not been enabled as of missing dependencies.");
                return;
            }

            // Load the config values
            ConfigUtils.processLoadFromConfiguration(module, module.getConfigString());

            if (module instanceof ListenerModule)
                ListenerModule.enable((ListenerModule) module);

            if (module instanceof PacketListenerModule)
                PacketListenerModule.enable((PacketListenerModule) module);

            if (module instanceof PluginMessageListenerModule)
                PluginMessageListenerModule.enable((PluginMessageListenerModule) module);

            module.enable();
            VerboseSender.getInstance().sendVerboseMessage(module.getConfigString() + " has been enabled.");
        } catch (final Exception e)
        {
            VerboseSender.getInstance().sendVerboseMessage(module.getConfigString() + " could not be enabled.", true, true);
            e.printStackTrace();
        }
    }

    /**
     * This disables the check according to its interfaces.
     */
    static void disableModule(final Module module)
    {
        try
        {
            if (module instanceof ListenerModule)
                ListenerModule.disable((ListenerModule) module);

            if (module instanceof PacketListenerModule)
                PacketListenerModule.disable((PacketListenerModule) module);

            if (module instanceof PluginMessageListenerModule)
                PluginMessageListenerModule.disable((PluginMessageListenerModule) module);

            module.disable();
            VerboseSender.getInstance().sendVerboseMessage(module.getConfigString() + " has been disabled.");
        } catch (final Exception e)
        {
            VerboseSender.getInstance().sendVerboseMessage(module.getConfigString() + " could not be disabled.", true, true);
            e.printStackTrace();
        }
    }

    /**
     * All additional chores during enabling that are not handled by the {@link Module} - subinterfaces.
     */
    default void enable() {}

    /**
     * All additional chores during disabling that are not handled by the {@link Module} - subinterfaces.
     */
    default void disable() {}

    /**
     * Gets the direct path representing this module in the config.
     */
    default String getConfigString()
    {
        return this.getModuleType().getConfigString();
    }

    /**
     * Gets the {@link ModuleType} of this {@link Module}
     */
    ModuleType getModuleType();
}
