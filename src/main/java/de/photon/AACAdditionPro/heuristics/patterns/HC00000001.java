package de.photon.AACAdditionPro.heuristics.patterns;

import de.photon.AACAdditionPro.heuristics.InputData;
import de.photon.AACAdditionPro.heuristics.Pattern;
import de.photon.AACAdditionPro.util.mathematics.MathUtils;

import java.util.Arrays;
import java.util.DoubleSummaryStatistics;
import java.util.Map;

/**
 * Checks for a plausible distance/time ratio to detect purposefully randomized inventory interactions
 */
public class HC00000001 extends Pattern
{
    /**
     * A threshold after which an inventory interaction is
     */
    private static final double IDLE_THRESHOLD = 600;

    public HC00000001()
    {
        super("HC000000001", new InputData[]{
                InputData.VALID_INPUTS.get('T'),
                InputData.VALID_INPUTS.get('X'),
                InputData.VALID_INPUTS.get('Y')
        });
    }

    @Override
    public Double analyse(Map<Character, InputData> inputData)
    {
        // See the constructor for the indices
        double[][] inputArray = this.provideInputData(inputData);

        // Use a offset sum to detect too consistent clicking.
        // orElse(0) is ok as the steam (and thus the array) must be empty to reach this part of code.
        double average = Arrays.stream(inputArray[0]).average().orElse(0);

        double offsetSum = 0;
        for (int i = 0; i < inputArray[0].length; i++)
        {
            final double offset = MathUtils.offset(inputArray[0][i], average);
            if (offset <= IDLE_THRESHOLD)
            {
                offsetSum += offset;
            }
        }

        final DoubleSummaryStatistics distanceSummary = new DoubleSummaryStatistics();
        for (int i = 0; i < inputArray[1].length; i++)
        {
            distanceSummary.accept(Math.hypot(inputArray[1][i], inputArray[2][i]));
        }

        return Math.tanh(((offsetSum / 150) * (distanceSummary.getMax() - distanceSummary.getMin())) / 4);
    }
}
