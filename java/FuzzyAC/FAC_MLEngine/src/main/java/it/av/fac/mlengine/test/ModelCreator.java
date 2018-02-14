/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.mlengine.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.neuroph.core.Connection;
import org.neuroph.core.Layer;
import org.neuroph.core.NeuralNetwork;
import org.neuroph.core.Neuron;
import org.neuroph.core.data.DataSet;
import org.neuroph.core.data.DataSetRow;
import org.neuroph.core.input.Min;
import org.neuroph.core.transfer.Gaussian;
import org.neuroph.nnet.MultiLayerPerceptron;
import org.neuroph.nnet.comp.layer.InputLayer;
import org.neuroph.nnet.learning.BackPropagation;

/**
 *
 * @author Diogo Regateiro <diogoregateiro@ua.pt>
 */
public class ModelCreator {

    private static final File MODEL = new File("ores.nnet");

    public static void main(String[] args) {
        NeuralNetwork neuralNetwork = null;
        if (MODEL.exists() && MODEL.isFile() && MODEL.canRead()) {
            try {
                System.out.println("Loading...");
                neuralNetwork = MultiLayerPerceptron.load(new FileInputStream(MODEL));
            } catch (FileNotFoundException ex) {
                Logger.getLogger(ModelCreator.class.getName()).log(Level.SEVERE, null, ex);
                System.exit(1);
            }
        } else {
            System.out.println("Init...");
            // create new perceptron network
            neuralNetwork = new NeuralNetwork();
            initNetwork(neuralNetwork);
            neuralNetwork.setLearningRule(new BackPropagation());

            System.out.println("Getting dataset...");
            // create training set
            DataSet dataset = DataSet.createFromFile("C:/Users/DiogoJosé/Documents/NetBeansProjects/FuzzyAC/java/WikipediaClient/prepDataset.csv", 8, 1, ",");
            DataSet[] datasets = dataset.createTrainingAndTestSubsets(80, 20);

            // learn the training set
            System.out.println("Learning...");
            neuralNetwork.learn(datasets[0]);

            // save the trained network into file
            System.out.println("Saving...");
            neuralNetwork.save("ores.nnet");

            testNetwork(neuralNetwork, datasets[1]);
        }
        System.out.println("Done!\n");

        printNetwork(neuralNetwork, 0, 0);
    }

    private static void printNetwork(NeuralNetwork neuralNetwork, double minIgnoreWeight, double maxIgnoreWeight) {
        Map<Double, String> outputOrder = new TreeMap<>((o1, o2) -> {
            return Double.compare(Math.abs(o1), Math.abs(o2)) * -1;
        });

        List<Connection> connections = new ArrayList<>();
        List<Layer> layers = neuralNetwork.getLayers();
        List<Neuron> neurons = new ArrayList<>();

        layers.stream().forEach((layer) -> neurons.addAll(layer.getNeurons()));
        neurons.stream().forEach((neuron) -> connections.addAll(neuron.getInputConnections()));

        connections.stream().forEach((connection)
                -> outputOrder.put(
                        connection.getWeight().value,
                        String.format("[%s] * %f --> [%s]",
                                connection.getFromNeuron().getLabel(),
                                connection.getWeight().value,
                                connection.getToNeuron().getLabel()))
        );

        outputOrder.values().stream().forEachOrdered((line) -> System.out.println(line));
    }

    private static void initNetwork(NeuralNetwork neuralNetwork) {
        Layer inputLayer = new InputLayer(8);
        Layer middleLayer = new Layer(28);
        Layer outputLayer = new Layer(1);

        String[] ilabels = new String[]{"DQ_OK", "DQ_ATTACK", "DQ_SPAM", "DQ_VANDALISM", "D_TRUE", "D_FALSE", "GF_TRUE", "GF_FALSE"};

        Neuron[] inputNeurons = new Neuron[8];

        for (int i = 0; i < ilabels.length; i++) {
            inputNeurons[i] = new Neuron(new Min(), new Gaussian());
            inputNeurons[i].setParentLayer(inputLayer);
            inputNeurons[i].setLabel(ilabels[i]);
            inputLayer.addNeuron(inputNeurons[i]);
        }

        Neuron[][] middleNeurons = new Neuron[8][8];

        for (int i = 0; i < inputNeurons.length; i++) {
            for (int j = i + 1; j < inputNeurons.length; j++) {
                middleNeurons[i][j] = new Neuron(new Min(), new Gaussian());
                middleNeurons[i][j].setLabel(ilabels[i] + " + " + ilabels[j]);
                middleNeurons[i][j].setParentLayer(middleLayer);
                middleLayer.addNeuron(middleNeurons[i][j]);
            }
        }

        Neuron outputNeuron = new Neuron(new Min(), new Gaussian());
        outputNeuron.setLabel("OUTPUT");
        outputNeuron.setParentLayer(outputLayer);
        outputLayer.addNeuron(outputNeuron);

        inputLayer.setParentNetwork(neuralNetwork);
        middleLayer.setParentNetwork(neuralNetwork);
        outputLayer.setParentNetwork(neuralNetwork);
        neuralNetwork.addLayer(inputLayer);
        neuralNetwork.addLayer(middleLayer);
        neuralNetwork.addLayer(outputLayer);
        neuralNetwork.setInputNeurons(Arrays.asList(inputNeurons));
        neuralNetwork.setOutputNeurons(Arrays.asList(outputNeuron));

        // Set connections
        for (int i = 0; i < inputNeurons.length; i++) {
            for (int j = i + 1; j < inputNeurons.length; j++) {
                neuralNetwork.createConnection(inputNeurons[i], middleNeurons[i][j], 1);
                neuralNetwork.createConnection(inputNeurons[j], middleNeurons[i][j], 1);
                neuralNetwork.createConnection(middleNeurons[i][j], outputNeuron, 1);
            }
        }

        neuralNetwork.randomizeWeights();
    }

    public static void testNetwork(NeuralNetwork nnet, DataSet testSet) {
        for (DataSetRow dataRow : testSet.getRows()) {
            nnet.setInput(dataRow.getInput());
            nnet.calculate();
            double[] networkOutput = nnet.getOutput();
            System.out.print("Input: " + Arrays.toString(dataRow.getInput()));
            System.out.println("Output: " + Arrays.toString(networkOutput));
        }
    }
}
