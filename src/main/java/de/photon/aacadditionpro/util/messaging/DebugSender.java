package de.photon.aacadditionpro.util.messaging;

import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionproold.AACAdditionPro;
import de.photon.aacadditionproold.events.ClientControlEvent;
import de.photon.aacadditionproold.events.PlayerAdditionViolationEvent;
import de.photon.aacadditionproold.util.commands.Placeholders;
import de.photon.aacadditionproold.util.files.FileUtil;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.val;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DebugSender implements Listener
{
    @Getter private static final DebugSender instance;
    private static final String EVENT_PRE_STRING = ChatColor.WHITE + "{player} " + ChatColor.GRAY;

    static {
        instance = new DebugSender();
        AACAdditionPro.getInstance().registerListener(instance);
    }

    private final boolean writeToFile = AACAdditionPro.getInstance().getConfig().getBoolean("Debug.file");
    private final boolean writeToConsole = AACAdditionPro.getInstance().getConfig().getBoolean("Debug.console");
    private final boolean writeToPlayers = AACAdditionPro.getInstance().getConfig().getBoolean("Debug.players");

    @Setter private volatile boolean allowedToRegisterTasks = true;
    // The File the verbose messages are written to.
    private File logFile = null;
    // Set to an impossible day of the year to make sure the logFile will be initialized.
    private int currentDayOfYear = -1;

    /**
     * Sets off a standard verbose message (no console forcing and not flagged as an error).
     *
     * @param s the message that will be sent
     */
    public void sendVerboseMessage(final String s)
    {
        sendVerboseMessage(s, false, false);
    }

    /**
     * This sets off a verbose message.
     *
     * @param s             the message that will be sent
     * @param force_console whether the verbose message should appear in the console even when verbose for console is deactivated.
     * @param error         whether the message should be marked as an error
     */
    public void sendVerboseMessage(final String s, final boolean force_console, final boolean error)
    {
        // Remove color codes
        val logMessage = ChatColor.stripColor(s);

        if (writeToFile) {
            try {
                // Get the logfile that is in use currently or create a new one if needed.
                val now = LocalDateTime.now();
                val dayOfYear = now.getDayOfYear();

                // Doesn't need to check for logFile == null as the currentDayOfYear will be -1 in the beginning.
                if (currentDayOfYear != dayOfYear || !logFile.exists()) {
                    currentDayOfYear = dayOfYear;
                    logFile = FileUtil.createFile(new File(AACAdditionPro.getInstance().getDataFolder().getPath() + "/logs/" + now.format(DateTimeFormatter.ISO_LOCAL_DATE) + ".log"));
                }

                // Reserve the required builder size.
                // Time length is always 12, together with 2 brackets and one space this will result in 15.
                val verboseMessage = new StringBuilder(15 + logMessage.length());
                // Add the beginning of the PREFIX
                verboseMessage.append('[');
                // Get the current time
                verboseMessage.append(now.format(DateTimeFormatter.ISO_LOCAL_TIME));

                // Add a 0 if it is too short
                // Technically only 12, but we already appended the "[", thus one more.
                while (verboseMessage.length() < 13) verboseMessage.append('0');

                // Add the rest of the PREFIX and the message
                verboseMessage.append(']').append(' ').append(logMessage).append('\n');

                // Log the message
                Files.write(logFile.toPath(), verboseMessage.toString().getBytes(), StandardOpenOption.APPEND);
            } catch (final IOException e) {
                AACAdditionPro.getInstance().getLogger().log(Level.SEVERE, "Something went wrong while trying to write to the log file.", e);
            }
        }

        if (writeToConsole || force_console) {
            AACAdditionPro.getInstance().getLogger().log(error ?
                                                         Level.SEVERE :
                                                         Level.INFO, logMessage);
        }

        // Prevent errors on disable as of scheduling
        if (allowedToRegisterTasks && writeToPlayers) {
            ChatMessage.sendSyncMessage(User.getDebugUsers(), s);
        }
    }

    @EventHandler
    public void onAdditionViolation(final PlayerAdditionViolationEvent event)
    {
        this.sendVerboseMessage(Placeholders.replacePlaceholders(EVENT_PRE_STRING + event.getMessage() + " | Vl: " + event.getVl() + " | TPS: {tps} | Ping: {ping}", event.getPlayer()));
    }

    @EventHandler
    public void onClientControl(final ClientControlEvent event)
    {
        this.sendVerboseMessage(Placeholders.replacePlaceholders(EVENT_PRE_STRING + event.getMessage(), event.getPlayer()));
    }
}