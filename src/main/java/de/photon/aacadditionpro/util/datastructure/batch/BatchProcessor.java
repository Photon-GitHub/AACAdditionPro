package de.photon.aacadditionpro.util.datastructure.batch;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import de.photon.aacadditionpro.modules.ViolationModule;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.util.datastructure.broadcast.BroadcastReceiver;
import de.photon.aacadditionpro.util.datastructure.broadcast.Broadcaster;
import lombok.Getter;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * An abstract representation of a processor of {@link Batch.Snapshot}s.
 * <p>
 * The {@link BatchProcessor} will subscribe to {@link Broadcaster}s to receive the {@link Batch.Snapshot}s and then
 * go on to process them.
 * <p>
 * As the {@link Batch.Snapshot}s can be received asynchronously, thread safety is important.
 */
public abstract class BatchProcessor<T> implements BroadcastReceiver<Batch.Snapshot<T>>
{
    @Getter
    private final ViolationModule module;

    @Unmodifiable
    private final Set<Broadcaster<Batch.Snapshot<T>>> broadcasters;

    protected BatchProcessor(ViolationModule module, Collection<Broadcaster<Batch.Snapshot<T>>> broadcasters)
    {
        this.module = module;

        Preconditions.checkArgument(broadcasters != null, "Tried to create BatchProcessor with null broadcasters.");
        Preconditions.checkArgument(!broadcasters.isEmpty(), "Tried to create BatchProcessor with empty broadcasters.");
        // The wrapping with ImmutableSet.copyOf does not reduce performance when broadcasters is already an
        // ImmutableSet (see copyOf() method).
        this.broadcasters = ImmutableSet.copyOf(broadcasters);
    }

    /**
     * This method represents the actual computation done on the batch.
     */
    public abstract void processBatch(User user, List<T> batch);

    public final void enable()
    {
        broadcasters.forEach(b -> b.subscribe(this));
        this.subEnable();
    }

    public final void disable()
    {
        broadcasters.forEach(b -> b.unsubscribe(this));
        this.subDisable();
    }

    protected void subEnable() {}

    protected void subDisable() {}
}
