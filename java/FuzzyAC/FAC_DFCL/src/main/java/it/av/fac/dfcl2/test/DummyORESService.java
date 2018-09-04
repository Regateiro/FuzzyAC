/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.dfcl2.test;

import it.av.fac.dfcl2.EIFS;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Diogo Regateiro <diogoregateiro@ua.pt>
 */
public class DummyORESService implements EIFS {

    private final String registeredVariable;
    private final List<double[]> scores;
    private final String[] labels;

    public DummyORESService(String inputVariableLabel) throws IOException {
        this.registeredVariable = inputVariableLabel;
        this.scores = new ArrayList<>();

        try (BufferedReader in = new BufferedReader(new FileReader(System.getProperty("user.home") + "/Documents/NetBeansProjects/FuzzyAC/java/WikipediaClient/autoDataset.csv"))) {
            labels = in.readLine().split(",");
        }
    }

    @Override
    public Map<String, Double> process(double inputValue) {
        Map<String, Double> ret = new HashMap<>();
        for (int i = 0; i < 6; i++) {
            ret.put(labels[i], 1.0);
        }
        return ret;
    }

    @Override
    public String getInputVariableLabel() {
        return registeredVariable;
    }

    @Override
    public int getExpectedInputVectorSize() {
        return 1;
    }

    @Override
    public int getOutputVectorSize() {
        return 6;
    }
    
    public double getExpectedResult(int inputValue) {
        return this.scores.get(inputValue)[6];
    }
    
    public int getDataSize() {
        return this.scores.size();
    }
}
