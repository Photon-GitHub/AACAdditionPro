package de.photon.AACAdditionPro.command.subcommands;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.InternalPermission;
import de.photon.AACAdditionPro.command.InternalCommand;
import de.photon.AACAdditionPro.command.subcommands.heuristics.CreateCommand;
import de.photon.AACAdditionPro.command.subcommands.heuristics.ListCommand;
import de.photon.AACAdditionPro.command.subcommands.heuristics.RemoveCommand;
import de.photon.AACAdditionPro.command.subcommands.heuristics.SaveCommand;
import de.photon.AACAdditionPro.command.subcommands.heuristics.TrainCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Queue;

public class HeuristicsCommand extends InternalCommand
{
    public static boolean heuristicsUnlocked()
    {
        return AACAdditionPro.getInstance().getConfig().getBoolean("InventoryHeuristics.enabled") &&
               AACAdditionPro.getInstance().getConfig().getBoolean("InventoryHeuristics.unlock_full_framework");
    }

    public HeuristicsCommand()
    {
        super("heuristics", InternalPermission.NEURAL, (byte) 0,
              new CreateCommand(),
              new ListCommand(),
              new RemoveCommand(),
              new SaveCommand(),
              new TrainCommand());
    }

    @Override
    protected void execute(CommandSender sender, Queue<String> arguments)
    {
        if (!heuristicsUnlocked())
        {
            sender.sendMessage(prefix + ChatColor.RED + "InventoryHeuristics framework is not loaded, enabled or unlocked.");
        }
        else
        {
            sender.sendMessage(ChatColor.GOLD + "------" + ChatColor.DARK_RED + " Heuristics - Pattern " + ChatColor.GOLD + "------");
            sender.sendMessage(ChatColor.GOLD + "Welcome to the heuristics framework.");

            final StringBuilder welcomeBuilder = new StringBuilder(32);
            welcomeBuilder.append(ChatColor.RED);
            welcomeBuilder.append("Possible commands: ");
            welcomeBuilder.append(ChatColor.GOLD);

            for (String s : this.getChildTabs())
            {
                welcomeBuilder.append(s);
                welcomeBuilder.append(" ,");
            }

            // Delete the last comma and space
            for (int i = 0; i < 2; i++)
            {
                welcomeBuilder.deleteCharAt(welcomeBuilder.length() - 1);
            }

            sender.sendMessage(welcomeBuilder.toString());
        }
    }

    @Override
    protected String[] getCommandHelp()
    {
        return new String[]{"Utilities for the InventoryHeuristics"};
    }

    @Override
    protected String[] getTabPossibilities()
    {
        return getChildTabs();
    }
}
