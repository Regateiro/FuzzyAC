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
import org.neuroph.core.events.NeuralNetworkEvent;
import org.neuroph.core.events.NeuralNetworkEventListener;
import org.neuroph.core.input.Min;
import org.neuroph.core.input.WeightedSum;
import org.neuroph.core.learning.SupervisedLearning;
import org.neuroph.core.transfer.Sigmoid;
import org.neuroph.nnet.MultiLayerPerceptron;
import org.neuroph.nnet.comp.layer.InputLayer;
import org.neuroph.nnet.comp.neuron.InputNeuron;
import org.neuroph.nnet.learning.BackPropagation;

/**
 *
 * @author Diogo Regateiro <diogoregateiro@ua.pt>
 */
public class NeurophTest {

    private static final File MODEL = new File("ores.nnet");
    private static final boolean LOAD_EXISTING_MODEL = true;
    private static final int ITERATIONS = 100;

    public static void main(String[] args) {
        NeuralNetwork neuralNetwork = null;
        if (LOAD_EXISTING_MODEL && MODEL.exists() && MODEL.isFile() && MODEL.canRead()) {
            try {
                System.out.println("Loading...");
                neuralNetwork = MultiLayerPerceptron.load(new FileInputStream(MODEL));
                System.out.println(" - Loaded " + MODEL.getAbsolutePath());
            } catch (FileNotFoundException ex) {
                Logger.getLogger(NeurophTest.class.getName()).log(Level.SEVERE, null, ex);
                System.exit(1);
            }
        } else {
            System.out.println("Init...");
            // create new perceptron network
            neuralNetwork = initNetwork();

            System.out.println("Getting dataset...");
            // create training set
            DataSet dataset = DataSet.createFromFile(System.getProperty("user.home") + "/Documents/NetBeansProjects/FuzzyAC/java/WikipediaClient/autoDataset.csv", 6, 1, ",");
            dataset.shuffle();
            DataSet[] datasets = dataset.createTrainingAndTestSubsets(75, 25);
            System.out.println(" - Loaded " + dataset.size() + " observations.");
            System.out.println(" - Is dataset supervised? " + dataset.isSupervised());
            System.out.println(" - Training dataset contains " + datasets[0].size() + " observations.");
            System.out.println(" - Validation dataset contains " + datasets[1].size() + " observations.");

            // learn the training set
            System.out.println("Learning...");
            SupervisedLearning rule = new BackPropagation();
            rule.setNeuralNetwork(neuralNetwork);
            rule.setTrainingSet(datasets[0]);
            for (int itr = 0; itr < ITERATIONS; itr++) {
                rule.doLearningEpoch(datasets[0]);
                System.out.println(String.format(" - Iteration %03d/%03d...", itr + 1, ITERATIONS));
                testNetwork(neuralNetwork, datasets[1]);
            }

            // save the trained network into file
            System.out.println("Saving...");
            //neuralNetwork.save("ores.nnet");
            System.out.println(" - Saved to ores.nnet");
        }
        System.out.println("Done!\n");

        printNetwork(neuralNetwork);
        
//        neuralNetwork.setInput(1.0, 0.0, 0.0, 0.0, 0.0, 1.0);
//        neuralNetwork.calculate();
//        System.out.println("\nOutput: ");
//        for(double output : neuralNetwork.getOutput()) {
//            System.out.print(output + " ");
//        }
    }

    private static void printNetwork(NeuralNetwork neuralNetwork) {
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

    private static NeuralNetwork initNetwork2(int numHiddenNeurons) {
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

    private static NeuralNetwork initNetwork() {
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
