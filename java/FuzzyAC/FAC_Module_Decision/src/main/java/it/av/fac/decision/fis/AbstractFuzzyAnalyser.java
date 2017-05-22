/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.decision.fis;

import it.av.fac.decision.util.Decision;
import it.av.fac.decision.util.DecisionResult;
import java.util.List;

/**
 * Abstract class used to analyze the inputs that get a certain permission decision. 
 * @author Diogo Regateiro
 */
public abstract class AbstractFuzzyAnalyser {
    public enum DecisionResultsToReturn {
        ALL, ONLY_GRANT, ONLY_DENY;
    }

    protected final FuzzyEvaluator feval;
    protected String permissionToAnalyse;
    protected int numberOfEvaluations;
    protected DecisionResultsToReturn decisionsToReturn;

    protected AbstractFuzzyAnalyser(FuzzyEvaluator feval) {
        this.feval = feval;
        this.numberOfEvaluations = 0;
    }

    /**
     * 
     * @param permission 
     * @param decisionsToResult
     * @return 
     */
    abstract public List<DecisionResult> analyse(String permission, DecisionResultsToReturn decisionsToResult);
    
    public void resetAnalyser() {
        this.numberOfEvaluations = 0;
    }
    
    public int getNumberOfEvaluations() {
        return numberOfEvaluations;
    }
}
