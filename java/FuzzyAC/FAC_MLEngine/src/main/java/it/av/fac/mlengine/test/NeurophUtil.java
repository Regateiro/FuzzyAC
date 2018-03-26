/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.mlengine.test;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import org.neuroph.core.Connection;
import org.neuroph.core.Layer;
import org.neuroph.core.NeuralNetwork;
import org.neuroph.core.Neuron;
import org.neuroph.core.data.DataSet;
import org.neuroph.core.data.DataSetRow;
import org.neuroph.core.input.Min;
import org.neuroph.core.input.WeightedSum;
import org.neuroph.core.transfer.Sigmoid;
import org.neuroph.nnet.MultiLayerPerceptron;
import org.neuroph.nnet.comp.layer.InputLayer;
import org.neuroph.nnet.comp.neuron.InputNeuron;

/**
 *
 * @author Diogo Regateiro <diogoregateiro@ua.pt>
 */
public class NeurophUtil {

    public static List<Entry<String, Double>> processNodesInfluence(NeuralNetwork neuralNetwork) {
        List<Entry<String, Double>> influences = new ArrayList<>();

        // Set identity input
        double[] identity = new double[neuralNetwork.getInputsCount()];
        Arrays.fill(identity, 1.0);
        neuralNetwork.setInput(identity);
        neuralNetwork.calculate();

        // Set the weighted inputs
        ((List<Neuron>) neuralNetwork.getOutputNeurons()).stream().forEach((Neuron outNeuron) -> {
            outNeuron.getInputConnections().forEach((Connection conn) -> {
                influences.add(new AbstractMap.SimpleImmutableEntry<>(conn.getFromNeuron().getLabel(), conn.getWeightedInput()));
            });
        });

        // Sort from max to min
        Collections.sort(influences, (a, b) -> {
            return a.getValue().compareTo(b.getValue()) * -1;
        });

        return influences;
    }

    public static void printNetwork(NeuralNetwork neuralNetwork) {
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
                        connection.getWeight().getValue(),
                        String.format("[%s] * %f --> [%s]",
                                connection.getFromNeuron().getLabel(),
                                connection.getWeight().getValue(),
                                connection.getToNeuron().getLabel()))
        );

        outputOrder.values().stream().forEachOrdered((line) -> System.out.println(line));
    }

    public static NeuralNetwork initNetwork2(int numHiddenNeurons) {
        MultiLayerPerceptron neuralNetwork = new MultiLayerPerceptron(6, numHiddenNeurons, 1);
        //neuralNetwork.connectInputsToOutputs();

        String[] ilabels = new String[]{"DQ_OK", "DQ_ATTACK", "DQ_SPAM", "DQ_VANDALISM", "DAMAGING", "GOOD_FAITH"};
        List<Neuron> ineurons = neuralNetwork.getInputNeurons();
        for (int i = 0; i < ineurons.size(); i++) {
            ineurons.get(i).setLabel(ilabels[i]);
        }

        List<Neuron> hneurons = ((Layer) neuralNetwork.getLayers().get(1)).getNeurons();
        int hiddenCount = 1;
        for (int i = 0; i < hneurons.size(); i++) {
            hneurons.get(i).setLabel("HIDDEN_" + hiddenCount++);
        }

        List<Neuron> oneurons = neuralNetwork.getOutputNeurons();
        oneurons.get(0).setLabel("OUTPUT");

        return neuralNetwork;
    }

    public static NeuralNetwork initNetwork() {
        NeuralNetwork neuralNetwork = new NeuralNetwork();

        System.out.println(" - Creating layers...");
        Layer inputLayer = new InputLayer(6);
        Layer middleLayer = new Layer(21);
        Layer outputLayer = new Layer(1);

        String[] ilabels = new String[]{"DQ_OK", "DQ_ATTACK", "DQ_SPAM", "DQ_VANDALISM", "DAMAGING", "GOOD_FAITH"};

        Neuron[] inputNeurons = new Neuron[6];
        System.out.println(" - Creating input neurons...");
        for (int i = 0; i < ilabels.length; i++) {
            inputNeurons[i] = new InputNeuron();
            inputNeurons[i].setParentLayer(inputLayer);
            inputNeurons[i].setLabel(ilabels[i]);
            inputLayer.addNeuron(inputNeurons[i]);
        }

        Neuron[] dHiddenNeurons = new Neuron[inputNeurons.length];
        System.out.println(" - Creating direct hidden neurons...");
        for (int i = 0; i < inputNeurons.length; i++) {
            dHiddenNeurons[i] = new Neuron(new Min(), new Sigmoid());
            dHiddenNeurons[i].setLabel("H_" + ilabels[i]);
            dHiddenNeurons[i].setParentLayer(middleLayer);
            middleLayer.addNeuron(dHiddenNeurons[i]);
        }

        Neuron[][] hiddenNeurons = new Neuron[inputNeurons.length][inputNeurons.length];
        System.out.println(" - Creating 2 by 2 hidden neurons...");
        for (int i = 0; i < inputNeurons.length; i++) {
            for (int j = i + 1; j < inputNeurons.length; j++) {
                hiddenNeurons[i][j] = new Neuron(new Min(), new Sigmoid());
                hiddenNeurons[i][j].setLabel(ilabels[i] + " + " + ilabels[j]);
                hiddenNeurons[i][j].setParentLayer(middleLayer);
                middleLayer.addNeuron(hiddenNeurons[i][j]);
            }
        }

        System.out.println(" - Creating output neurons...");
        Neuron outputNeuron = new Neuron(new WeightedSum(), new Sigmoid());
        outputNeuron.setLabel("OUTPUT");
        outputNeuron.setParentLayer(outputLayer);
        outputLayer.addNeuron(outputNeuron);

        System.out.println(" - Setting up layers on network...");
        inputLayer.setParentNetwork(neuralNetwork);
        middleLayer.setParentNetwork(neuralNetwork);
        outputLayer.setParentNetwork(neuralNetwork);
        neuralNetwork.addLayer(inputLayer);
        neuralNetwork.addLayer(middleLayer);
        neuralNetwork.addLayer(outputLayer);
        neuralNetwork.setInputNeurons(Arrays.asList(inputNeurons));
        neuralNetwork.setOutputNeurons(Arrays.asList(outputNeuron));

        // Set connections
        System.out.println(" - Setting up connections between neurons...");
        for (int i = 0; i < inputNeurons.length; i++) {
            for (int j = i + 1; j < inputNeurons.length; j++) {
                neuralNetwork.createConnection(inputNeurons[i], hiddenNeurons[i][j], 1);
                neuralNetwork.createConnection(inputNeurons[j], hiddenNeurons[i][j], 1);
                neuralNetwork.createConnection(hiddenNeurons[i][j], outputNeuron, 1);
            }
            neuralNetwork.createConnection(inputNeurons[i], dHiddenNeurons[i], 1);
            neuralNetwork.createConnection(dHiddenNeurons[i], outputNeuron, 1);
        }

        System.out.println(" - Randomizing weights...");
        neuralNetwork.randomizeWeights();
        return neuralNetwork;
    }

    public static void testNetwork(NeuralNetwork nnet, DataSet testSet) {
        double tp = 0, tn = 0, fp = 0, fn = 0;
        for (DataSetRow dataRow : testSet.getRows()) {
            nnet.setInput(dataRow.getInput());
            nnet.calculate();
            double networkOutput = nnet.getOutput()[0];
            double expectedOutput = dataRow.getDesiredOutput()[0];
            if (expectedOutput > 0.5) {
                if (Math.abs(expectedOutput - networkOutput) < 0.5) {
                    tp++;
                } else {
                    fn++;
                }
            } else {
                if (Math.abs(expectedOutput - networkOutput) < 0.5) {
                    tn++;
                } else {
                    fp++;
                }
            }
        }

        System.out.println("   --> Precision: " + (tp / (tp + fp)));
        System.out.println("   --> True Positive Rate: " + (tp / (tp + fn)));
        System.out.println("   --> True Negative Rate: " + (tn / (tn + fp)));
        System.out.println("   --> Accuracy: " + ((tp + tn) / (tp + tn + fp + fn)));
    }
}
