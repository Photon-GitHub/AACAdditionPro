package de.photon.aacadditionpro.util.messaging;

import de.photon.aacadditionpro.AACAdditionPro;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ChatMessage
{
    public static final String AACADDITIONPRO_PREFIX = ChatColor.AQUA + "[AACAdditionPro] " + ChatColor.GRAY;

    /**
     * Sends a message with the AACAdditionPro prefix to a single recipient.
     */
    public static void sendMessage(CommandSender recipient, String message)
    {
        recipient.sendMessage(AACADDITIONPRO_PREFIX + message);
    }

    /**
     * Sends a message with the AACAdditionPro prefix to a single recipient.
     * This method should be called asynchronously, else use {@link #sendMessage(CommandSender, String)}
     */
    public static void sendSyncMessage(CommandSender recipient, String message)
    {
        Bukkit.getScheduler().runTask(AACAdditionPro.getInstance(), () -> ChatMessage.sendMessage(recipient, message));
    }

    /**
     * Sends a message with the AACAdditionPro prefix to multiple recipients, caching the message for less String
     * concatenations.
     */
    public static void sendMessage(final Iterable<? extends CommandSender> senders, final String message)
    {
        val prefixedMessage = AACADDITIONPRO_PREFIX + message;
        for (CommandSender cs : senders) cs.sendMessage(prefixedMessage);
    }

    /**
     * Sends a message with the AACAdditionPro prefix to multiple recipients, caching the message for less String
     * concatenations.
     * This method should be called asynchronously, else use {@link #sendMessage(Iterable, String)}
     */
    public static void sendSyncMessage(final Iterable<? extends CommandSender> senders, final String message)
    {
        Bukkit.getScheduler().runTask(AACAdditionPro.getInstance(), () -> ChatMessage.sendMessage(senders, message));
    }

    /**
     * Sends the "You don't have permission to do that." message with prefix to a recipient.
     */
    public static void sendNoPermissionMessage(CommandSender recipient)
    {
        sendMessage(recipient, "You don't have permission to do that.");
    }

    /**
     * Sends the "The specified player could not be found." message with prefix to a recipient.
     */
    public static void sendPlayerNotFoundMessage(CommandSender recipient)
    {
        sendMessage(recipient, "The specified player could not be found.");
    }
}
