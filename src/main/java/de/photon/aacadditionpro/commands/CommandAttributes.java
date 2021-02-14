package de.photon.aacadditionpro.commands;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import de.photon.aacadditionpro.InternalPermission;
import de.photon.aacadditionpro.util.mathematics.MathUtil;
import de.photon.aacadditionpro.util.messaging.ChatMessage;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permissible;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CommandAttributes
{
    private final SortedMap<String, InternalCommand> childCommands;
    private final List<String> commandHelp;
    private final InternalPermission permission;
    private final int minArguments;
    private final int maxArguments;

    public static Builder builder()
    {
        return new Builder();
    }

    /**
     * Checks if the given argument count is valid by the attributes.
     *
     * @param arguments the argument count.
     *
     * @return <code>true</code> iff minArguments <= arguments <= maxArguments
     */
    public boolean argumentsInRange(int arguments)
    {
        return MathUtil.inRange(minArguments, maxArguments, arguments);
    }

    public boolean hasPermission(Permissible permissible)
    {
        // This will automatically return true for null-permissions.
        return InternalPermission.hasPermission(permissible, this.permission);
    }

    /**
     * Sends the complete command help to a player.
     */
    public void sendCommandHelp(CommandSender sender)
    {
        for (String line : this.commandHelp) {
            ChatMessage.sendMessage(sender, line);
        }
    }

    /**
     * Factory for building a {@link CommandAttributes} instance.
     */
    public static class Builder
    {
        private final SortedMap<String, InternalCommand> childCommands = new TreeMap<>();
        private final List<String> commandHelp = new ArrayList<>();
        private InternalPermission permission = null;
        private int minArguments = 0;
        private int maxArguments = 100;

        /**
         * The minimum arguments of the command that should be enforced.
         */
        public Builder minArguments(int minArguments)
        {
            this.minArguments = minArguments;
            return this;
        }

        /**
         * The maximum arguments of the command that should be enforced.
         */
        public Builder maxArguments(int maxArguments)
        {
            this.maxArguments = maxArguments;
            return this;
        }

        /**
         * Shortcut for setting both the minimum and maximum arguments to the same value.
         */
        public Builder exactArguments(int exactArg)
        {
            this.minArguments = exactArg;
            this.maxArguments = exactArg;
            return this;
        }

        /**
         * Sets the permission of the command.
         */
        public Builder setPermission(InternalPermission permission)
        {
            this.permission = permission;
            return this;
        }

        /**
         * Directly sets the command help.
         */
        public Builder setCommandHelp(List<String> commandHelp)
        {
            this.commandHelp.clear();
            this.commandHelp.addAll(commandHelp);
            return this;
        }

        /**
         * Directly sets the command help.
         */
        public Builder setCommandHelp(String... commandHelp)
        {
            this.commandHelp.clear();
            this.commandHelp.addAll(Arrays.asList(commandHelp));
            return this;
        }

        /**
         * Add a single line to the command help.
         */
        public Builder addCommandHelpLine(String line)
        {
            this.commandHelp.add(line);
            return this;
        }

        /**
         * Directly sets the command help.
         */
        public Builder setChildCommands(Collection<InternalCommand> commands)
        {
            this.childCommands.clear();
            commands.forEach(this::addChildCommand);
            return this;
        }

        /**
         * Directly sets the command help.
         */
        public Builder setChildCommands(InternalCommand... commands)
        {
            this.childCommands.clear();
            Arrays.stream(commands).forEach(this::addChildCommand);
            return this;
        }

        /**
         * Add a single line to the command help.
         */
        public Builder addChildCommand(InternalCommand command)
        {
            this.childCommands.put(command.name, command);
            return this;
        }

        public CommandAttributes build()
        {
            if (!this.childCommands.isEmpty()) {
                this.addCommandHelpLine("Subcommands of this command are: " + String.join(", ", this.childCommands.keySet()));
            }
            return new CommandAttributes(ImmutableSortedMap.copyOfSorted(childCommands), ImmutableList.copyOf(commandHelp), permission, minArguments, maxArguments);
        }
    }
}