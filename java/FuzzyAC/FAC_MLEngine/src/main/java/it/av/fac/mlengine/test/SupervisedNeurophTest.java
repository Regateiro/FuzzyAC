/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.mlengine.test;

import static it.av.fac.mlengine.test.NeurophUtil.initNetwork;
import static it.av.fac.mlengine.test.NeurophUtil.printNetwork;
import static it.av.fac.mlengine.test.NeurophUtil.testNetwork;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.neuroph.core.NeuralNetwork;
import org.neuroph.core.data.DataSet;
import org.neuroph.core.learning.SupervisedLearning;
import org.neuroph.nnet.MultiLayerPerceptron;
import org.neuroph.nnet.learning.BackPropagation;

/**
 *
 * @author Diogo Regateiro <diogoregateiro@ua.pt>
 */
public class SupervisedNeurophTest {

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
                Logger.getLogger(SupervisedNeurophTest.class.getName()).log(Level.SEVERE, null, ex);
                System.exit(1);
            }
        } else {
            System.out.println("Init...");
            // create new perceptron network
            neuralNetwork = initNetwork();

            System.out.println("Getting dataset...");
            // create training set
            DataSet dataset = DataSet.createFromFile(System.getProperty("user.home") + "/Documents/NetBeansProjects/FuzzyAC/java/WikipediaClient/autoDataset.csv", 6, 1, ",", true);
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
            neuralNetwork.save("ores.nnet");
            System.out.println(" - Saved to ores.nnet");
        }
        System.out.println("Done!\n");

        System.out.println("Influence by hidden node...");
        NeurophUtil.processNodesInfluence(neuralNetwork).forEach((influence) -> {
            System.out.println(String.format("%+09.5f -> %s", influence.getValue(), influence.getKey()));
        });
    }
}
