package de.photon.aacadditionpro.util.violationlevels;

import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.Multiset;
import lombok.Getter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

class ViolationLevelMultiSet implements Listener
{
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Getter
    private final Multiset<UUID> multiset = ConcurrentHashMultiset.create();

    ViolationLevelMultiSet(final long decayMilliseconds)
    {
        this(decayMilliseconds, 1);
    }

    ViolationLevelMultiSet(final long decayMilliseconds, final int occurences)
    {
        // Might need to have a vl manager without vl decrease
        if (decayMilliseconds > 0) {
            // Schedule the decay with 3000 milliseconds to free startup.
            scheduler.scheduleAtFixedRate(() -> this.decay(occurences), 3000, decayMilliseconds, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Decrements the vl of every player.
     */
    private void decay(int occurences)
    {
        // Decrement the vl of every player.
        for (UUID uuid : multiset.elementSet()) multiset.remove(uuid, occurences);
    }

    @EventHandler
    public void onQuit(final PlayerQuitEvent event)
    {
        this.multiset.setCount(event.getPlayer().getUniqueId(), 0);
    }
}
