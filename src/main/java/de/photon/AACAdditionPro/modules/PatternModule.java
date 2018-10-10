package de.photon.AACAdditionPro.modules;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketEvent;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.util.VerboseSender;
import lombok.AccessLevel;
import lombok.Getter;

import java.util.Set;
import java.util.function.BiFunction;

/**
 * This indicates a {@link Module} which can be split up into different parts called {@link Pattern}s.
 */
public interface PatternModule extends Module
{
    /**
     * @return a {@link Set} of all {@link Pattern}s managed by this {@link PatternModule}
     */
    Set<Pattern> getPatterns();

    static void enablePatterns(final PatternModule module)
    {
        for (Pattern pattern : module.getPatterns()) {
            Module.enableModule(pattern);
        }
    }

    static void disablePatterns(final PatternModule module)
    {
        for (Pattern pattern : module.getPatterns()) {
            Module.disableModule(pattern);
        }
    }

    /**
     * Represents a single {@link Pattern} that is hold by a {@link PatternModule}
     */
    abstract class Pattern<T, U> implements BiFunction<T, U, Integer>, Module
    {
        @Getter(AccessLevel.PROTECTED)
        private boolean enabled = false;

        protected String message = null;

        @Override
        public Integer apply(T t, U u)
        {
            if (this.isEnabled()) {
                final int process = process(t, u);

                if (process > 0 && message != null) {
                    VerboseSender.getInstance().sendVerboseMessage(message);
                }
                return process;
            }
            return 0;
        }

        @Override
        public abstract String getConfigString();

        /**
         * Actually process the data.
         *
         * @return the vlIncrease of the {@link PatternModule}.
         */
        protected abstract int process(T t, U u);

        /**
         * The action to be performed if the cancel_vl is surpassed
         */
        public void cancelAction(T t, U u) {}

        @Override
        public boolean shouldNotify()
        {
            return false;
        }

        @Override
        public void enable()
        {
            this.enabled = true;
        }

        @Override
        public void disable()
        {
            this.enabled = false;
        }
    }

    /**
     * Special handling class for {@link Pattern}s that use packets.
     * This class will automatically ensure that only the correct packets are used.
     */
    abstract class PacketPattern extends Pattern<User, PacketEvent>
    {
        private final Set<PacketType> packetTypesProcessed;

        /**
         * Constructs a new {@link PacketPattern}.
         *
         * @param packetTypesProcessed the {@link PacketType}s this {@link PacketPattern} will use.
         */
        protected PacketPattern(Set<PacketType> packetTypesProcessed)
        {
            this.packetTypesProcessed = packetTypesProcessed;
        }

        @Override
        public Integer apply(User user, PacketEvent packetEvent)
        {
            if (this.isEnabled() &&
                packetTypesProcessed.contains(packetEvent.getPacketType()))
            {
                final int process = process(user, packetEvent);

                if (process > 0 && message != null) {
                    VerboseSender.getInstance().sendVerboseMessage(message);
                }
                return process;
            }
            return 0;
        }
    }
}
