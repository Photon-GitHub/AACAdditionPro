package de.photon.aacadditionproold.util.violationlevels;

import de.photon.aacadditionproold.AACAdditionPro;
import de.photon.aacadditionproold.events.PlayerAdditionViolationEvent;
import de.photon.aacadditionproold.modules.ModuleType;
import de.photon.aacadditionproold.util.commands.CommandUtils;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ViolationLevelManagement
{
    /**
     * The {@link Map} violation-levels of all the players.
     */
    protected final ViolationLevelMap violationLevels;

    /**
     * A {@link List} of {@link Threshold}s which is guaranteed to be sorted.
     */
    protected final ThresholdList thresholds;

    /**
     * The {@link ModuleType} of the handler, used for reload
     */
    private final ModuleType moduleType;

    /**
     * A constant to calculate the score from the vl.
     */
    private final double aacScoreMultiplier;

    /**
     * Create a new {@link ViolationLevelManagement}
     *
     * @param moduleType the {@link ModuleType} of the module this {@link ViolationLevelManagement} is being used by.
     * @param decayTicks the time in ticks until the vl of a player is decreased by one. If this is negative no decrease will happen.
     */
    public ViolationLevelManagement(final ModuleType moduleType, final long decayTicks)
    {
        // The ModuleType of the check
        this.moduleType = moduleType;

        ModuleType.VL_MODULETYPES.add(moduleType);

        this.violationLevels = new ViolationLevelMap(decayTicks);
        // Listener registration as of the PlayerQuitEvent
        AACAdditionPro.getInstance().registerListener(this.violationLevels);

        this.aacScoreMultiplier = AACAdditionPro.getInstance().getConfig().getDouble(this.moduleType.getConfigString() + ".aacscoremultiplier");

        // Load the thresholds.
        this.thresholds = new ThresholdList(Threshold.loadThresholds(moduleType.getConfigString() + ".thresholds"));
    }

    /**
     * Flags a {@link Player} in the violationLevelManagement with the amount of vl_increase
     *
     * @param player      the player that should be flagged.
     * @param cancel_vl   the ViolationLevel up from which onCancel is run. Set to -1 to disable
     * @param onCancel    a {@link Runnable} that is executed if the vl is higher that cancel_vl
     * @param specialCode a {@link Runnable} to define special code such as critical_vl. Contrary to normal code it is only run if the event is not cancelled.
     */
    public void flag(final Player player, final int cancel_vl, final Runnable onCancel, final Runnable specialCode)
    {
        flag(player, 1, cancel_vl, onCancel, specialCode);
    }

    /**
     * Flags a {@link Player} in the violationLevelManagement with the amount of vlIncrease
     *
     * @param player           the player that should be flagged.
     * @param vlIncrease       how much the vl should be increased.
     * @param cancelVl         the ViolationLevel up from which onCancel is run. Set to -1 to disable
     * @param onCancel         a {@link Runnable} that is executed if the vl is higher that cancelVl
     * @param runIfEventPassed a {@link Runnable} to define special code such as critical_vl. Contrary to normal code it is only run if the event is not cancelled.
     */
    public void flag(final Player player, final int vlIncrease, final int cancelVl, final Runnable onCancel, final Runnable runIfEventPassed)
    {
        flag(player, this.moduleType.getViolationMessage(), vlIncrease, cancelVl, onCancel, runIfEventPassed);
    }

    /**
     * Flags a {@link Player} in the violationLevelManagement with the amount of vlIncrease
     *
     * @param player           the player that should be flagged.
     * @param vlIncrease       how much the vl should be increased.
     * @param cancelVl         the ViolationLevel up from which onCancel is run. Set to -1 to disable
     * @param onCancel         a {@link Runnable} that is executed if the vl is higher that cancelVl
     * @param runIfEventPassed a {@link Runnable} to define special code such as critical_vl. Contrary to normal code it is only run if the event is not cancelled.
     */
    public void flag(final Player player, final String message, final int vlIncrease, final int cancelVl, final Runnable onCancel, final Runnable runIfEventPassed)
    {
        // Prevent unnecessary flagging.
        if (vlIncrease <= 0) {
            return;
        }

        // Create and call the event.
        final PlayerAdditionViolationEvent playerAdditionViolationEvent = PlayerAdditionViolationEvent.build(player, this.moduleType, this.getVL(player.getUniqueId()) + vlIncrease, message).call();

        if (!playerAdditionViolationEvent.isCancelled()) {
            this.addVL(player, vlIncrease);

            //Cancel
            if (cancelVl > 0 && cancelVl <= this.getVL(player.getUniqueId())) {
                onCancel.run();
            }

            runIfEventPassed.run();
        }
    }

    /**
     * @param uuid the {@link UUID} of the {@link Player} whose vl should be returned.
     *
     * @return the vl of the given uuid.
     */
    public final int getVL(final UUID uuid)
    {
        return violationLevels.getOrDefault(uuid, 0);
    }

    /**
     * Calculates the score given to AAC.
     */
    public final double getAACScore(final UUID uuid)
    {
        return this.getVL(uuid) * this.aacScoreMultiplier;
    }

    /**
     * Sets the vl of a player.
     *
     * @param player the {@link Player} whose vl should be set
     * @param newVl  the new vl of the player.
     */
    public void setVL(final Player player, final int newVl)
    {
        final int oldVl = this.getVL(player.getUniqueId());

        violationLevels.put(player.getUniqueId(), newVl);

        // setVL is also called when decreasing the vl
        // thus we must prevent double punishment
        if (oldVl < newVl) {
            this.punishPlayer(player, oldVl, newVl);
        }
    }

    /**
     * Adds an {@link Integer} to the vl of a player. The number can be negative, this will decrease the vl then.
     *
     * @param player the {@link Player} whose vl should be set
     * @param vl     the vl to be added to the current vl.
     */
    private void addVL(final Player player, final int vl)
    {
        this.setVL(player, this.getVL(player.getUniqueId()) + vl);
    }

    /**
     * Used to execute the commands that are defined in the config section CHECK_NAME.thresholds
     *
     * @param player the {@link Player} that should be punished and that should be used to apply the placeholders
     * @param fromVl the last vl of the player before the addition and the searching-range for command.
     */
    private void punishPlayer(final Player player, final int fromVl, final int toVl)
    {
        // Only schedule the command execution if the plugin is loaded and when we do not use AAC's feature handling.
        if (AACAdditionPro.getInstance().isLoaded() && AACAdditionPro.getInstance().getAacapi() == null) {
            this.thresholds.forEachThreshold(fromVl, toVl, threshold -> {
                // Thresholds is a random access list, therefore get() is fine here.
                for (String command : threshold.getCommandList()) {
                    // Calling of the event + Sync command execution
                    CommandUtils.executeCommandWithPlaceholders(command, player, moduleType);
                }
            });
        }
    }
}