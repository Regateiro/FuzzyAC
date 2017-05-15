/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.decision.fis;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Diogo Regateiro
 */
public abstract class AbstractFuzzyAnalyser {

    protected final FuzzyEvaluator feval;
    protected final Map<String, String> outputBuffer;
    protected String permissionToAnalyse;
    protected int numberOfEvaluations;

    protected AbstractFuzzyAnalyser(FuzzyEvaluator feval) {
        this.feval = feval;
        this.outputBuffer = new HashMap<>();
        this.numberOfEvaluations = 0;
    }

    abstract public void analyse(String permission);
    
    public void resetAnalyser() {
        this.outputBuffer.clear();
        this.numberOfEvaluations = 0;
    }
    
    public int getNumberOfEvaluations() {
        return numberOfEvaluations;
    }
}
