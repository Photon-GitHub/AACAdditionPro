package de.photon.AACAdditionPro.user.data;

import de.photon.AACAdditionPro.checks.subchecks.InventoryHeuristics;
import de.photon.AACAdditionPro.oldheuristics.NeuralPattern;

import java.util.DoubleSummaryStatistics;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class InventoryHeuristicsData
{
    public String trainingLabel = null;
    public NeuralPattern trainedPattern = null;

    private Map<String, Double> patternMap = new ConcurrentHashMap<>();

    /**
     * This should be called prior to {@link #setPatternConfidence(String, double)} to make the confidences decay with legit actions.
     */
    public void decayCycle()
    {
        patternMap.forEach((patternName, confidence) -> {
            final double newConfidence = confidence - 0.1;

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
     * Sets the confidence of a {@link NeuralPattern}.
     * If the present confidence is higher than the new one, no confidence will be set.
     *
     * @param patternName the name of the {@link NeuralPattern}
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
        DoubleSummaryStatistics summaryStatistics = new DoubleSummaryStatistics();
        patternMap.forEach((patternName, value) -> {
            // Make sure too many low-confidence violations won't flag high global confidence
            // -> use cubic function.
            summaryStatistics.accept((value * value * value) * 1.2D * Objects.requireNonNull(InventoryHeuristics.getPatternByName(patternName), "Invalid pattern name: " + patternName).getWeight());
        });

        // Make sure that the result is greater or equal than 0.
        return Math.max(0D, Math.tanh(summaryStatistics.getSum() - 0.42));
    }
}
