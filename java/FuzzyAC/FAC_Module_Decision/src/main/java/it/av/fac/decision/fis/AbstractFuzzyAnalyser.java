/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.decision.fis;

import it.av.fac.decision.util.DecisionResult;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Diogo Regateiro
 */
public abstract class AbstractFuzzyAnalyser {

    protected final FuzzyEvaluator feval;
    protected DecisionResult lastResult;
    protected String permissionToAnalyse;
    protected int numberOfEvaluations;

    protected AbstractFuzzyAnalyser(FuzzyEvaluator feval) {
        this.feval = feval;
        this.lastResult = null;
        this.numberOfEvaluations = 0;
    }

    abstract public List<DecisionResult> analyse(String permission);
    
    public void resetAnalyser() {
        this.lastResult = null;
        this.numberOfEvaluations = 0;
    }
    
    public int getNumberOfEvaluations() {
        return numberOfEvaluations;
    }
}
