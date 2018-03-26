/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.mlengine.test;

import com.google.common.collect.Sets;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import quickdt.data.HashMapAttributes;
import quickdt.data.Instance;
import quickdt.predictiveModels.decisionTree.Tree;
import quickdt.predictiveModels.decisionTree.TreeBuilder;

/**
 *
 * @author Diogo Regateiro <diogoregateiro@ua.pt>
 */
public class RandomForest {

    private static final String datasetPath = System.getProperty("user.home") + "/Documents/NetBeansProjects/FuzzyAC/java/WikipediaClient/autoDataset.csv";

    public static void main(String[] args) throws Exception {
        System.out.println("Loading dataset...");
        final Set<Instance> dataset = readFromCSV(datasetPath);
        System.out.println(" --> Done!");
        
        System.out.println("Building predictive model...");
        TreeBuilder treeBuilder = new TreeBuilder();
        treeBuilder.maxDepth(3);
        Tree tree = treeBuilder.buildPredictiveModel(dataset);
        System.out.println(" --> Done!");

        System.out.println("\n######## TREE DUMP ########");
        tree.dump(System.out);
        System.out.println("###########################\n");
    }

    private static Set<Instance> readFromCSV(String datasetPath) {
        Set<Instance> dataset = Sets.newHashSet();
        
        try (BufferedReader in = new BufferedReader(new FileReader(datasetPath))) {
            String line;
            String[] header = in.readLine().split("[,]");
            while ((line = in.readLine()) != null) {
                String[] fields = line.split("[,]");
                dataset.add(HashMapAttributes.create(
                        header[0], Double.parseDouble(fields[0]),
                        header[1], Double.parseDouble(fields[1]),
                        header[2], Double.parseDouble(fields[2]),
                        header[3], Double.parseDouble(fields[3]),
                        header[4], Double.parseDouble(fields[4]),
                        header[5], Double.parseDouble(fields[5])
                ).classification(fields[6].equals("1") ? "good_edit" : "bad_edit"));
            }
        } catch (IOException | NumberFormatException ex) {
            Logger.getLogger(RandomForest.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return dataset;
    }
}
