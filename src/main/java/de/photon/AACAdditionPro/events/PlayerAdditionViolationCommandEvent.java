package de.photon.AACAdditionPro.events;

import de.photon.AACAdditionPro.ModuleType;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

@SuppressWarnings("unused")
public class PlayerAdditionViolationCommandEvent extends PlayerEvent implements Cancellable
{
    private static final HandlerList handlers = new HandlerList();
    private final ModuleType moduleType;
    private boolean cancelled;
    private String command;

    public PlayerAdditionViolationCommandEvent(final Player player, final String command, final ModuleType moduleType)
    {
        super(player);
        this.command = command;
        this.moduleType = moduleType;
    }

    //Needed for 1.8.8
    public static HandlerList getHandlerList()
    {
        return handlers;
    }

    public ModuleType getModuleType()
    {
        return moduleType;
    }

    public String getCommand()
    {
        return command;
    }

    public void setCommand(final String command)
    {
        this.command = command;
    }

    @Override
    public boolean isCancelled()
    {
        return cancelled;
    }

    @Override
    public void setCancelled(final boolean b)
    {
        cancelled = b;
    }

    @Override
    public HandlerList getHandlers()
    {
        return handlers;
    }
}
