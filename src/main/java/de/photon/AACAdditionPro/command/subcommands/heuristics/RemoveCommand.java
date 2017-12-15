package de.photon.AACAdditionPro.command.subcommands.heuristics;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.InternalPermission;
import de.photon.AACAdditionPro.checks.subchecks.InventoryHeuristics;
import de.photon.AACAdditionPro.command.InternalCommand;
import de.photon.AACAdditionPro.heuristics.Pattern;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Optional;
import java.util.Queue;

public class RemoveCommand extends InternalCommand
{
    public RemoveCommand()
    {
        super("remove", InternalPermission.NEURAL_CREATE, (byte) 2);
    }

    @Override
    protected void execute(CommandSender sender, Queue<String> arguments)
    {
        if (AACAdditionPro.getInstance().getConfig().getBoolean("InventoryHeuristics.enabled"))
        {
            final String patternName = arguments.remove();

            final Optional<Pattern> patternToDelete = InventoryHeuristics.getPATTERNS().stream().filter(pattern -> pattern.getName().equals(patternName)).findAny();

            if (patternToDelete.isPresent())
            {
                InventoryHeuristics.getPATTERNS().remove(patternToDelete.get());

                sender.sendMessage(ChatColor.GOLD + "------" + ChatColor.DARK_RED + " Heuristics - Pattern " + ChatColor.GOLD + "------");
                sender.sendMessage(ChatColor.GOLD + "Deleted pattern \"" + patternName + "\"");
            }
            else
            {
                sender.sendMessage(ChatColor.GOLD + "------" + ChatColor.DARK_RED + " Heuristics - Pattern " + ChatColor.GOLD + "------");
                sender.sendMessage(ChatColor.GOLD + "Pattern \"" + patternName + "\"" + " could not be found.");
            }
        }
    }

    @Override
    protected String[] getCommandHelp()
    {
        return new String[]{"Removes a pattern from the active ones."};
    }

    @Override
    protected String[] getTabPossibilities()
    {
        return getChildTabs();
    }
}
