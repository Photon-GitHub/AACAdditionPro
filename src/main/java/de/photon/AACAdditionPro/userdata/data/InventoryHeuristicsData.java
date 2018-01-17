package de.photon.AACAdditionPro.userdata.data;

import java.util.HashMap;
import java.util.Map;

public class InventoryHeuristicsData
{
    private Map<String, Double> patternMap = new HashMap<>();

    /**
     * This should be called prior to {@link #setPatternConfidence(String, double)} to make the confidences decay with legit actions.
     */
    public void decayCycle()
    {
        patternMap.forEach((patternName, confidence) -> {
            final double newConfidence = confidence - 10;

            if (newConfidence > 0)
            {
                patternMap.put(patternName, newConfidence);
            }
            else
            {
                patternMap.remove(patternName);
            }
        });
    }

    /**
     * Sets the confidence of a {@link de.photon.AACAdditionPro.heuristics.Pattern}.
     * If the present confidence is higher than the new one, no confidence will be set.
     *
     * @param patternName the name of the {@link de.photon.AACAdditionPro.heuristics.Pattern}
     * @param confidence  the new confidence.
     */
    public void setPatternConfidence(final String patternName, final double confidence)
    {
        // Only set if the confidence level increases.
        if (patternMap.getOrDefault(patternName, 0D) < confidence)
        {
            patternMap.put(patternName, confidence);
        }
    }

    /**
     * Calculates the global confidence based on the sum of the single confidences.
     */
    public double calculateGlobalConfidence()
    {
        double sum = 0;
        for (Double value : patternMap.values())
        {
            sum += value;
        }

        return Math.tanh(sum - 0.2D);
    }
}
