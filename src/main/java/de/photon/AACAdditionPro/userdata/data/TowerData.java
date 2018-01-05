package de.photon.AACAdditionPro.userdata.data;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.ModuleType;
import de.photon.AACAdditionPro.userdata.User;
import de.photon.AACAdditionPro.util.storage.datawrappers.BlockPlace;
import de.photon.AACAdditionPro.util.storage.datawrappers.TowerBlockPlace;
import de.photon.AACAdditionPro.util.storage.management.ConditionalBuffer;
import de.photon.AACAdditionPro.util.world.BlockUtils;
import lombok.Getter;


/**
 * Used to store {@link BlockPlace}s. The {@link TimeData} is used for timeouts.
 */
public class TowerData extends TimeData
{
    private static int BUFFER_SIZE = AACAdditionPro.getInstance().getConfig().getInt(ModuleType.TOWER.getConfigString() + ".buffer_size");

    @Getter
    protected final ConditionalBuffer<TowerBlockPlace> blockPlaces = new ConditionalBuffer<TowerBlockPlace>(BUFFER_SIZE)
    {
        @Override
        protected boolean verifyObject(TowerBlockPlace object)
        {
            return blockPlaces.isEmpty() ||
                   BlockUtils.isNext(blockPlaces.peek().getBlock(), object.getBlock(), false);
        }
    };

    public TowerData(User theUser)
    {
        super(false, theUser);
    }

    /**
     * Adds a {@link TowerBlockPlace} to the buffer
     *
     * @param towerBlockPlace The {@link TowerBlockPlace} which should be added.
     *
     * @return whether or not the {@link TowerBlockPlace} was accepted by {@link ConditionalBuffer#verifyObject(Object)}
     */
    public boolean bufferBlockPlace(final TowerBlockPlace towerBlockPlace)
    {
        return blockPlaces.bufferObject(towerBlockPlace);
    }

    /**
     * Used to calculate the average time span between the {@link TowerBlockPlace}s in the buffer
     * Also clears the buffer.
     *
     * @return the average time between {@link TowerBlockPlace}s.
     */
    public double calculateAverageTime()
    {
        // fraction[0] is the enumerator
        // fraction[1] is the divider
        final double[] fraction = new double[2];

        this.blockPlaces.clearLastTwoObjectsIteration(
                (last, current) ->
                {
                    fraction[0] += (last.getTime() - current.getTime());
                    fraction[1]++;
                });
        return fraction[0] / fraction[1];
    }
}