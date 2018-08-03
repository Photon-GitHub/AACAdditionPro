package de.photon.AACAdditionPro.util.mathematics;


import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoublePredicate;

public final class MathUtils
{
    /**
     * Simple method to know if a number is close to another number
     *
     * @param a     The first number
     * @param b     The second number
     * @param range The maximum search range
     *
     * @return true if the numbers are in range of one another else false
     */
    public static boolean roughlyEquals(final double a, final double b, final double range)
    {
        return offset(a, b) <= range;
    }

    /**
     * Calculates the sum of the offsets in the array based on a value.
     *
     * @param inputs     the array of which the offset sum should be calculated
     * @param offsetBase the reference point for the single offsets
     * @param predicate  whether or not a certain offset should be added.
     *
     * @return the sum of the offsets in the array.
     */
    public static double offsetSum(final double[] inputs, final double offsetBase, final DoublePredicate predicate)
    {
        double offsetSum = 0;
        for (double input : inputs)
        {
            final double offset = MathUtils.offset(input, offsetBase);
            if (predicate.test(offset))
            {
                offsetSum += offset;
            }
        }
        return offsetSum;
    }

    /**
     * Simple method to calculate the absolute offset of two numbers.
     *
     * @return the absolute offset, always positive or 0 if the numbers are equal.
     */
    public static double offset(final double a, final double b)
    {
        return a > b ? (a - b) : (b - a);
    }

    /**
     * Generates a new random integer.
     *
     * @param min            the result will at least be this parameter
     * @param randomBoundary the result will at most be min + randomBoundary
     *
     * @return the resulting random integer
     */
    public static int randomBoundaryInt(int min, int randomBoundary)
    {
        return min + ThreadLocalRandom.current().nextInt(randomBoundary);
    }

    /**
     * Generates a new random double.
     *
     * @param min            the result will at least be this parameter
     * @param randomBoundary the result will at most be min + randomBoundary
     *
     * @return the resulting random double
     */
    public static double randomBoundaryDouble(double min, double randomBoundary)
    {
        return min + ThreadLocalRandom.current().nextDouble(randomBoundary);
    }
}
