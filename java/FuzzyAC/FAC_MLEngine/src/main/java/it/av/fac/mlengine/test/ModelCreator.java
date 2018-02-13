/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.mlengine.test;

import java.util.ArrayList;
import java.util.List;
import org.neuroph.core.Connection;
import org.neuroph.core.NeuralNetwork;
import org.neuroph.core.Neuron;
import org.neuroph.core.data.DataSet;
import org.neuroph.core.data.DataSetRow;
import org.neuroph.nnet.MultiLayerPerceptron;

/**
 *
 * @author Diogo Regateiro <diogoregateiro@ua.pt>
 */
public class ModelCreator {

    public static void main(String[] args) {
        int InNeurons = 2;
        int OutNeurons = 1;
        int HiddenNeurons = (InNeurons * (InNeurons - 1)) / 2;
        
        // create new perceptron network
        NeuralNetwork neuralNetwork = new MultiLayerPerceptron(InNeurons, HiddenNeurons, OutNeurons);

        // create training set
        DataSet trainingSet = new DataSet(2, 1);

        // add training data to training set (logical OR function)
        trainingSet.addRow(new DataSetRow(new double[]{0, 0},
                new double[]{0}));
        trainingSet.addRow(new DataSetRow(new double[]{0, 1},
                new double[]{1}));
        trainingSet.addRow(new DataSetRow(new double[]{1, 0},
                new double[]{1}));
        trainingSet.addRow(new DataSetRow(new double[]{1, 1},
                new double[]{1}));

        // learn the training set
        neuralNetwork.learn(trainingSet);

        List<Connection> connections = getNetworkConnections(neuralNetwork);

        System.out.println("");

        // save the trained network into file
        //neuralNetwork.save("or_perceptron.nnet");
    }

    private static List<Connection> getNetworkConnections(NeuralNetwork neuralNetwork) {
        return getNetworkConnections(neuralNetwork, neuralNetwork.getOutputNeurons());
    }

    private static List<Connection> getNetworkConnections(NeuralNetwork neuralNetwork, List<Neuron> toNeurons) {
        List<Connection> ret = new ArrayList<>();

        // get the connections
        toNeurons.stream().filter((toNeuron) -> (!neuralNetwork.getInputNeurons().contains(toNeuron)))
                .map((toNeuron) -> toNeuron.getInputConnections())
                .forEachOrdered((connections) -> {
                    List<Neuron> fromNeurons = new ArrayList<>();
                    connections.stream().forEach((conn) -> fromNeurons.add(conn.getFromNeuron()));
                    ret.addAll(connections);
                    ret.addAll(getNetworkConnections(neuralNetwork, fromNeurons));
                });

        return ret;
    }
}
