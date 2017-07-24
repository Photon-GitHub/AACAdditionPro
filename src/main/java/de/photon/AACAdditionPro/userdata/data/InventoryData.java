package de.photon.AACAdditionPro.userdata.data;

import de.photon.AACAdditionPro.userdata.User;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class InventoryData extends TimeData
{
    /**
     * The last slot a person clicked.<br>
     * This variable is used to prevent false positives based on spam-clicking one slot.
     */
    public int lastSlot = 0;

    public Material lastMaterial = Material.BEDROCK;

    public InventoryData(final User theUser)
    {
        super(true, theUser, 0, 0);
    }

    public boolean notRecentlyOpened(final long milliseconds)
    {
        return !this.recentlyUpdated(0, milliseconds);
    }

    public boolean recentlyClicked(final long milliseconds)
    {
        return this.recentlyUpdated(1, milliseconds);
    }

    public boolean hasOpenInventory()
    {
        return this.getTimeStamp(0) != 0;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void on(final PlayerDeathEvent event)
    {
        if (theUser.refersToUUID(event.getEntity().getUniqueId())) {
            this.nullifyTimeStamp(0);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void on(final PlayerRespawnEvent event)
    {
        if (theUser.refersToUUID(event.getPlayer().getUniqueId())) {
            this.nullifyTimeStamp(0);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void on(final InventoryOpenEvent event)
    {
        if (theUser.refersToUUID(event.getPlayer().getUniqueId()) &&
            theUser.getPlayer().getOpenInventory().getType() != InventoryType.CRAFTING)
        {
            this.updateTimeStamp(0);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void on(final InventoryClickEvent event)
    {
        if (theUser.refersToUUID(event.getWhoClicked().getUniqueId()) &&
            event.getSlotType() != InventoryType.SlotType.QUICKBAR)
        {
            if (this.getTimeStamp(0) == 0) {
                this.updateTimeStamp(0);
            }
            this.updateTimeStamp(1);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void on(final InventoryCloseEvent event)
    {
        if (theUser.refersToUUID(event.getPlayer().getUniqueId())) {
            this.nullifyTimeStamp(0);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void on(final PlayerTeleportEvent event)
    {
        if (theUser.refersToUUID(event.getPlayer().getUniqueId())) {
            this.nullifyTimeStamp(0);
        }
    }

    @EventHandler
    public void on(final PlayerChangedWorldEvent event)
    {
        if (theUser.refersToUUID(event.getPlayer().getUniqueId())) {
            this.nullifyTimeStamp(0);
        }
    }
}