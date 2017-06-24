package de.photon.AACAdditionPro.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class APILoadedEvent extends Event
{
    private static final HandlerList handlers = new HandlerList();

    // Needed for 1.8.8
    public static HandlerList getHandlerList()
    {
        return handlers;
    }

    @Override
    public HandlerList getHandlers()
    {
        return handlers;
    }
}
