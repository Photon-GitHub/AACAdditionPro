package de.photon.AACAdditionPro.checks.subchecks;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.AdditionHackType;
import de.photon.AACAdditionPro.checks.AACAdditionProCheck;
import de.photon.AACAdditionPro.userdata.User;
import de.photon.AACAdditionPro.userdata.UserManager;
import de.photon.AACAdditionPro.util.storage.management.ViolationLevelManagement;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class InventoryHit implements Listener, AACAdditionProCheck
{
    private final ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getAdditionHackType(), 100L);

    private int cancel_vl;

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void on(final EntityDamageByEntityEvent event)
    {
        if (event.getDamager() instanceof Player) {
            final User user = UserManager.getUser(event.getDamager().getUniqueId());

            // Not bypassed
            if (user == null || user.isBypassed()) {
                return;
            }

            // Is in Inventory (Detection)
            if (user.getInventoryData().hasOpenInventory() &&
                // Have the inventory opened for some time
                user.getInventoryData().notRecentlyOpened(1000) &&
                // Is a hit-attack
                event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK)
            {
                vlManager.flag(user.getPlayer(), cancel_vl, () -> event.setCancelled(true), () -> {});
            }
        }
    }

    @Override
    public ViolationLevelManagement getViolationLevelManagement()
    {
        return vlManager;
    }

    @Override
    public AdditionHackType getAdditionHackType()
    {
        return AdditionHackType.INVENTORY_HIT;
    }

    @Override
    public void subEnable()
    {
        cancel_vl = AACAdditionPro.getInstance().getConfig().getInt(this.getAdditionHackType().getConfigString() + ".cancel_vl");
    }
}