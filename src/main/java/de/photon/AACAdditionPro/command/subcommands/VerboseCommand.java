package de.photon.AACAdditionPro.command.subcommands;

import de.photon.AACAdditionPro.InternalPermission;
import de.photon.AACAdditionPro.command.InternalCommand;
import de.photon.AACAdditionPro.userdata.User;
import de.photon.AACAdditionPro.userdata.UserManager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Queue;
import java.util.Set;

public class VerboseCommand extends InternalCommand
{
    public VerboseCommand(String name, InternalPermission permission, boolean onlyPlayers, byte minArguments, byte maxArguments)
    {
        super("verbose", InternalPermission.VERBOSE, true, (byte) 0, (byte) 1);
    }

    /**
     * Used to send the right message
     *
     * @param sender  the {@link CommandSender} of the command, the message will be sent to him
     * @param enabled the message-format (was Verbose disabled or enabled)
     */
    private static void sendToggleMessage(final CommandSender sender, final boolean enabled)
    {
        sender.sendMessage(prefix + ChatColor.GOLD + "Verbose " + (enabled ?
                                                                   (ChatColor.DARK_GREEN + "enabled") :
                                                                   (ChatColor.RED + "disabled")
        ));
    }

    @Override
    protected void execute(CommandSender sender, Queue<String> arguments)
    {
        final User user = UserManager.getUser(((Player) sender).getUniqueId());

        if (user == null) {
            return;
        }

        if (arguments.size() == 1) {
            switch (arguments.peek()) {
                case "on":
                    user.verbose = true;
                    sendToggleMessage(sender, true);
                    return;
                case "off":
                    user.verbose = false;
                    sendToggleMessage(sender, false);
                    return;
                default:
                    break;
            }
        } else {
            //Toggle mode
            user.verbose = !user.verbose;
            sendToggleMessage(sender, user.verbose);
        }
    }

    @Override
    protected String[] getCommandHelp()
    {
        return new String[]{"Used to toggle the verbose messages on and off for oneself."};
    }

    @Override
    protected Set<InternalCommand> getChildCommands()
    {
        return null;
    }

    @Override
    protected String[] getTabPossibilities()
    {
        return new String[]{
                "on",
                "off"
        };
    }
}
