/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.mlengine.test;

import de.daslaboratorium.machinelearning.classifier.Classifier;
import de.daslaboratorium.machinelearning.classifier.bayes.BayesClassifier;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Diogo Regateiro <diogoregateiro@ua.pt>
 */
public class BayesTest {
    
    private static final String datasetPath = System.getProperty("user.home") + "/Documents/NetBeansProjects/FuzzyAC/java/WikipediaClient/autoDataset.csv";

    public static void main(String[] args) throws FileNotFoundException {
        // Create a new bayes classifier with string categories and string features.
        Classifier<Double, String> bayes = new BayesClassifier<>();
        bayes.setMemoryCapacity(1000000);
        
        System.out.println("Training Naive Bayes...");        
        try(BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(datasetPath)))) {
            in.readLine();
            String line;
            while((line = in.readLine()) != null) {
                String[] fields = line.split(",");
                bayes.learn(fields[6].equals("1") ? "Allow" : "Stop", Arrays.asList(
                        Double.valueOf(fields[0]), 
                        Double.valueOf(fields[1]), 
                        Double.valueOf(fields[2]), 
                        Double.valueOf(fields[3]), 
                        Double.valueOf(fields[4]), 
                        Double.valueOf(fields[5])
                ));
            }
        } catch (IOException ex) {
            Logger.getLogger(BayesTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println(" ---> Done!");     
        System.exit(0);
        
        // Here are two unknown sentences to classify.
        //String[] unknownText1 = "today is a sunny day".split("\\s");
        //String[] unknownText2 = "there will be rain".split("\\s");

        //System.out.println( // will output "positive"
        //        bayes.classify(Arrays.asList(unknownText1)).getCategory());
        //System.out.println( // will output "negative"
        //        bayes.classify(Arrays.asList(unknownText2)).getCategory());

        // Get more detailed classification result.
        //((BayesClassifier<Double, String>) bayes).classifyDetailed(
        //        Arrays.asList(unknownText1));

        // Change the memory capacity. New learned classifications (using
        // the learn method) are stored in a queue with the size given
        // here and used to classify unknown sentences.
    }
}
