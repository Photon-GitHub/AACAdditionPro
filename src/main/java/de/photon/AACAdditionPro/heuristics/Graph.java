package de.photon.AACAdditionPro.heuristics;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.ModuleType;
import de.photon.AACAdditionPro.exceptions.NeuralNetworkException;

import java.io.Serializable;

/**
 * The graph of a neural network including the matrix.
 * This class is {@link Serializable} to allow easy saving of the data.
 */
public class Graph implements Serializable
{
    private static final double TRAIN_PARAMETER = AACAdditionPro.getInstance().getConfig().getDouble(ModuleType.INVENTORY_HEURISTICS.getConfigString() + ".framework.train_parameter");
    private static final double MOMENTUM_PARAMETER = AACAdditionPro.getInstance().getConfig().getDouble(ModuleType.INVENTORY_HEURISTICS.getConfigString() + ".framework.momentum_parameter");

    // The main matrix containing the weights of all connections
    // Use Wrapper class to be able to set a value to null
    private Double[][] matrix;
    private double[][] weightChangeMatrix;

    // Working array does not need to be serialized
    private double[] neurons;
    private double[] activatedNeurons;
    private double[] deltas;

    private int[] neuronsInLayers;

    /**
     * Constructs a new Graph
     */
    Graph(int[] neuronsInLayers)
    {
        this.neuronsInLayers = neuronsInLayers;

        int sumOfNeurons = 0;
        for (int sizeOfLayer : this.neuronsInLayers)
        {
            sumOfNeurons += sizeOfLayer;
        }

        this.matrix = new Double[sumOfNeurons][sumOfNeurons];
        this.weightChangeMatrix = new double[sumOfNeurons][sumOfNeurons];

        this.neurons = new double[sumOfNeurons];
        this.activatedNeurons = new double[sumOfNeurons];
        this.deltas = new double[sumOfNeurons];

        // Invalidate every connection
        for (int i = 0; i < this.matrix.length; i++)
        {
            for (int j = 0; j < matrix.length; j++)
            {
                this.matrix[i][j] = null;
            }
        }

        int currentNeuron = 0;
        int nextLayerFirstNeuron;
        int nextNextLayerFirstNeuron = this.neuronsInLayers[0];

        // Do not iterate over the last layer as output neurons do not need any additional connections.
        for (int layer = 1; layer < this.neuronsInLayers.length; layer++)
        {
            nextLayerFirstNeuron = nextNextLayerFirstNeuron;
            nextNextLayerFirstNeuron += this.neuronsInLayers[layer];

            while (currentNeuron < nextLayerFirstNeuron)
            {
                for (int to = nextLayerFirstNeuron; to < nextNextLayerFirstNeuron; to++)
                {
                    // Validate allowed connections.
                    this.matrix[currentNeuron][to] = 0D;
                }

                // Increment the neuron here as the matrix otherwise sets the connections of the next neuron.
                currentNeuron++;
            }
        }
    }

    /**
     * Set the input values of the {@link Graph} and {@link #calculate()}.
     *
     * @param inputValues the input values as a matrix with the different tests being the second index and the
     *                    different input sources (e.g. time delta, slot distance, etc.) being the first index.
     *
     * @return the output values of the output neurons.
     */
    public double analyse(double[][] inputValues)
    {
        for (int i = 0; i < neurons.length; i++)
        {
            neurons[i] = 0;
            activatedNeurons[i] = 0;
        }

        int inputIndex = 0;
        for (double[] inputArray : inputValues)
        {
            for (double input : inputArray)
            {
                this.neurons[inputIndex++] = input;
            }
        }

        // Not >= as the inputIndex is as well increased by one in the end.
        if (inputIndex > this.neuronsInLayers[0])
        {
            throw new NeuralNetworkException("Wrong amount of input values.");
        }
        return this.calculate();
    }

    /**
     * Train the neural network.
     *
     * @param inputValues the input values as a matrix with the different tests being the first index and the
     *                    different input sources (e.g. time delta, slot distance, etc.) being the second index.
     * @param cheating    whether or not the data is regarded as cheating.
     */
    public void train(double[][] inputValues, boolean cheating)
    {
        // Only calculate so that the neurons array is updated.
        this.analyse(inputValues);

        for (int currentNeuron = matrix.length - 1; currentNeuron >= 0; currentNeuron--)
        {
            deltas[currentNeuron] = activatedNeurons[currentNeuron] * (1 - activatedNeurons[currentNeuron]);

            // Deltas depend on the neuron class.
            switch (this.classifyNeuron(currentNeuron))
            {
                case INPUT:
                    break;
                case HIDDEN:
                    double sum = 0;
                    final int[] indices = nextLayerIndexBoundaries(currentNeuron);

                    // f'(netinput) * Sum(delta_(toHigherLayer) * matrix[thisNeuron][toHigherLayer])
                    for (int higherLayerNeuron = indices[0]; higherLayerNeuron <= indices[1]; higherLayerNeuron++)
                    {
                        // matrix[currentNeuron][i] should never be null as every neuron is connected with all the
                        // neurons of the previous layer.
                        sum += (deltas[higherLayerNeuron] * matrix[currentNeuron][higherLayerNeuron]);
                    }

                    deltas[currentNeuron] *= sum;
                    break;
                case OUTPUT:
                    // f'(netInput) * (a_wanted - a_real)
                    deltas[currentNeuron] *= (cheating ?
                                              1D :
                                              0D) - activatedNeurons[currentNeuron];
                    break;
            }

            for (int from = matrix.length - 1; from >= 0; from--)
            {
                if (matrix[from][currentNeuron] != null)
                {
                    // Calculate the new weightChange.
                    // The old weight change is in here as a part of the momentum.
                    weightChangeMatrix[from][currentNeuron] = (1 - MOMENTUM_PARAMETER) * TRAIN_PARAMETER * activatedNeurons[from] * deltas[currentNeuron] +
                                                              (MOMENTUM_PARAMETER * weightChangeMatrix[from][currentNeuron]);

                    matrix[from][currentNeuron] += weightChangeMatrix[from][currentNeuron];
                }
            }
        }
    }

    /**
     * Calculate the current {@link Graph}.
     * Prior to this method call the input neurons should be set.
     *
     * @return the output value of the activated output neuron.
     */
    private double calculate()
    {
        // Perform all the adding
        for (int currentNeuron = 0; currentNeuron < this.matrix.length; currentNeuron++)
        {
            // Activation function
            this.activatedNeurons[currentNeuron] = applyActivationFunction(this.neurons[currentNeuron]);

            // Forward - pass of the values
            for (int to = 0; to < this.matrix.length; to++)
            {
                // Forbid a connection in null - values
                if (matrix[currentNeuron][to] != null)
                {
                    this.neurons[to] += (this.activatedNeurons[currentNeuron] * this.matrix[currentNeuron][to]);
                }
            }
        }

        // The last neuron is the output neuron.
        return this.activatedNeurons[this.activatedNeurons.length - 1];
    }

    /**
     * @return an array of integers. <br>
     * [0] -> the start index of the next layer <br>
     * [1] -> the end index of the next layer
     */
    private int[] nextLayerIndexBoundaries(int index)
    {
        // 0 is the correct start here as the first layer starts at 0.
        int startingIndex = 0;

        // The OutputNeurons should not be used / throw an exception, which is guaranteed via < length.
        for (int i = 0; i < neuronsInLayers.length; i++)
        {
            startingIndex += neuronsInLayers[i];

            if (startingIndex > index)
            {
                // - 1 as the algorithm first of all calculates the starting index of the next layer and the ending
                // index is that starting index - 1
                return new int[]{startingIndex, startingIndex + (neuronsInLayers[i + 1] - 1)};
            }
        }
        throw new NeuralNetworkException("Cannot identify the next layer of neuron " + index + " out of " + matrix.length);
    }

    /**
     * Determines whether a neuron is an input neuron, a hidden neuron or an output neuron
     *
     * @param indexOfNeuron the index of the neuron which should be classified (index of the matrix).
     */
    private NeuronType classifyNeuron(int indexOfNeuron)
    {
        if (indexOfNeuron < neuronsInLayers[0])
        {
            return NeuronType.INPUT;
        }

        // Only the last neuron is output.
        if (indexOfNeuron == matrix.length - 1)
        {
            return NeuronType.OUTPUT;
        }

        return NeuronType.HIDDEN;
    }

    /**
     * Classifier of a neuron
     */
    private enum NeuronType
    {
        INPUT,
        HIDDEN,
        OUTPUT
    }

    /**
     * Calculated the activated value of a neuron.
     *
     * @param input the netinput of the neuron
     *
     * @return the value of the activated neuron or the derived neuron, depending on the parameter derived.
     */
    private static double applyActivationFunction(double input)
    {
        return 1 / (1 + Math.pow(Math.E, (-input)));
    }
}
