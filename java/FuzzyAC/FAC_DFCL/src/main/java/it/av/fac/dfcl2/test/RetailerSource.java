/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.dfcl2.test;

import it.av.fac.dfcl2.ESource;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Regateiro
 */
public class RetailerSource implements ESource {

    private final String outputLabel;
    private final String sourceLabel;
    
    public RetailerSource(String sourceLabel, String outputLabel) {
        this.outputLabel = outputLabel;
        this.sourceLabel = sourceLabel;
    }

    @Override
    public Map<String, Double> process(Map<String, Double> inputValues) {
        Map<String, Double> ret = new HashMap<>();
        ret.put(outputLabel, Math.floor(Math.random() * 2));
        return ret;
    }

    @Override
    public String getSourceLabel() {
        return sourceLabel;
    }

    @Override
    public List<String> getInputVariableLabels() {
        return Arrays.asList("ProductID");
    }

    @Override
    public int getExpectedInputVectorSize() {
        return 1;
    }

    @Override
    public int getOutputVectorSize() {
        return 1;
    }
    
}
