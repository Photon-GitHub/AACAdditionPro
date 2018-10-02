package de.photon.AACAdditionPro.modules.checks.inventory;

import de.photon.AACAdditionPro.modules.ModuleType;
import de.photon.AACAdditionPro.modules.PatternModule;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.util.files.configs.LoadFromConfiguration;
import de.photon.AACAdditionPro.util.inventory.InventoryUtils;
import lombok.Getter;
import me.konsolas.aac.api.AACAPIProvider;
import org.bukkit.GameMode;
import org.bukkit.event.inventory.InventoryClickEvent;

class MultiInteractionPattern extends PatternModule.Pattern<User, InventoryClickEvent>
{
    @LoadFromConfiguration(configPath = ".cancel_vl")
    @Getter
    private static int cancelVl;

    @LoadFromConfiguration(configPath = ".max_ping")
    private double max_ping;
    @LoadFromConfiguration(configPath = ".min_tps")
    private double min_tps;

    @Override
    protected int process(User user, InventoryClickEvent event)
    {
        // Creative-clear might trigger this.
        if ((user.getPlayer().getGameMode() == GameMode.SURVIVAL || user.getPlayer().getGameMode() == GameMode.ADVENTURE) &&
            // Minimum TPS before the check is activated as of a huge amount of fps
            AACAPIProvider.getAPI().getTPS() > min_tps &&
            // Minimum ping
            (max_ping < 0 || AACAPIProvider.getAPI().getPing(user.getPlayer()) <= max_ping) &&
            // False positive: Click-spamming on the same slot
            event.getRawSlot() != user.getInventoryData().getLastRawSlot())
        {
            // Default vl to 3
            int addedVl = 3;
            // Time in ms that will flag if it has not passed
            int enforcedTicks = 0;

            switch (event.getAction()) {
                // ------------------------------------------ Exemptions -------------------------------------------- //
                case NOTHING:
                    // Nothing happens, therefore exempted
                case UNKNOWN:
                    // Unknown reason might not be save to handle
                case COLLECT_TO_CURSOR:
                    // False positive with collecting all items of one type in the inventory
                    return 0;
                // ------------------------------------------ Normal -------------------------------------------- //
                case HOTBAR_SWAP:
                case HOTBAR_MOVE_AND_READD:
                    addedVl = 1;
                    enforcedTicks = 1;
                    // Enough distance to keep false positives at bay.
                    if (InventoryUtils.distanceBetweenSlots(event.getRawSlot(), user.getInventoryData().getLastRawSlot(), event.getClickedInventory().getType()) >= 3) {
                        return 0;
                    }
                    break;

                case DROP_ALL_SLOT:
                case DROP_ONE_SLOT:
                    // Drops might be faster than the others.
                    enforcedTicks = 1;
                    break;

                case PICKUP_ALL:
                case PICKUP_SOME:
                case PICKUP_HALF:
                case PICKUP_ONE:
                case PLACE_ALL:
                case PLACE_SOME:
                case PLACE_ONE:
                    // No false positives to check for.
                    addedVl = 3;

                    enforcedTicks = (InventoryUtils.distanceBetweenSlots(event.getRawSlot(), user.getInventoryData().getLastRawSlot(), event.getClickedInventory().getType()) < 4) ?
                                    1 :
                                    5;
                    break;

                case DROP_ALL_CURSOR:
                case DROP_ONE_CURSOR:
                case CLONE_STACK:
                    // No false positives to check for.
                    enforcedTicks = 2;
                    break;

                case MOVE_TO_OTHER_INVENTORY:
                    // Last material false positive.
                    if (user.getInventoryData().getLastMaterial() == event.getCurrentItem().getType()) {
                        return 0;
                    }

                    // Depending on the distance of the clicks.
                    enforcedTicks = (InventoryUtils.distanceBetweenSlots(event.getRawSlot(), user.getInventoryData().getLastRawSlot(), event.getClickedInventory().getType()) < 4) ?
                                    1 :
                                    2;
                    break;

                case SWAP_WITH_CURSOR:
                    switch (event.getSlotType()) {
                        // Armor slots are not eligible for less ticks as of quick change problems with the feet slot.
                        // No false positives possible in fuel or crafting slot as it is only one slot which is separated from others
                        case FUEL:
                        case RESULT:
                            enforcedTicks = 4;
                            break;

                        // Default tested value.
                        default:
                            enforcedTicks = 2;
                            break;
                    }
                    break;
            }

            // Convert ticks to millis.
            // 25 to account for server lag.
            if (user.getInventoryData().recentlyClicked(25 + (enforcedTicks * 50))) {
                return addedVl;
            }
        }
        return 0;
    }

    @Override
    public void cancelAction(User user, InventoryClickEvent event)
    {
        event.setCancelled(true);
        InventoryUtils.syncUpdateInventory(user.getPlayer());
    }

    @Override
    public String getConfigString()
    {
        return this.getModuleType().getConfigString() + ".parts.MultiInteraction";
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.INVENTORY;
    }
}
