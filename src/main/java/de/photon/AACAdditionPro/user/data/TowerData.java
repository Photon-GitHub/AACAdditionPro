package de.photon.AACAdditionPro.user.data;

import de.photon.AACAdditionPro.user.TimeData;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.user.datawrappers.BlockPlace;
import de.photon.AACAdditionPro.user.datawrappers.TowerBlockPlace;
import de.photon.AACAdditionPro.util.datastructures.ConditionalBuffer;
import de.photon.AACAdditionPro.util.world.BlockUtils;
import lombok.Getter;

/**
 * Used to store {@link BlockPlace}s. The {@link TimeData} is used for timeouts.
 */
public class TowerData extends TimeData
{
    // Default buffer size is 6, being well tested.
    private static final int BUFFER_SIZE = 6;

    @Getter
    private final ConditionalBuffer<TowerBlockPlace> blockPlaces = new ConditionalBuffer<TowerBlockPlace>(BUFFER_SIZE)
    {
        @Override
        protected boolean verifyObject(TowerBlockPlace object)
        {
            return this.getDeque().isEmpty() || BlockUtils.isNext(this.getDeque().peek().getBlock(), object.getBlock(), false);
        }
    };

    public TowerData(final User user)
    {
        super(user, 0);
    }

    /**
     * Used to calculate the average and expected time span between the {@link TowerBlockPlace}s in the buffer.
     * Also clears the buffer.
     *
     * @return an array with the following contents:<br>
     * [0] = Expected time <br>
     * [1] = Real time <br>
     */
    public double[] calculateTimes()
    {
        final double[] result = new double[2];
        // -1 because there is one pop to fill the "last" variable in the beginning.
        final int divisor = this.blockPlaces.getDeque().size() - 1;
        this.blockPlaces.clearLastTwoObjectsIteration((last, current) -> {
            result[0] += current.calculateDelay();
            result[1] += (last.getTime() - current.getTime());
        });
        result[0] /= divisor;
        result[1] /= divisor;
        return result;
    }

    @Override
    public void unregister()
    {
        this.blockPlaces.getDeque().clear();
        super.unregister();
    }
}