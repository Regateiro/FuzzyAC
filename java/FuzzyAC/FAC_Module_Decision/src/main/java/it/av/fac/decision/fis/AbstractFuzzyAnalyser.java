/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.decision.fis;

import it.av.fac.decision.util.decision.IDecisionMaker;
import it.av.fac.decision.util.handlers.IResultHandler;
import java.util.Collections;
import java.util.List;

/**
 * Abstract class used to analyze the inputs that get a certain permission
 * decision.
 *
 * @author Diogo Regateiro <diogoregateiro@ua.pt>
 */
public abstract class AbstractFuzzyAnalyser {

    public enum DecisionResultsToReturn {
        ALL, ONLY_GRANT, ONLY_DENY;
    }

    protected final BDFIS bdfis;
    protected String permissionToAnalyse;
    protected int numberOfEvaluations;
    protected IDecisionMaker decisionMaker;
    protected IResultHandler handler;
    protected List<String> order;

    protected AbstractFuzzyAnalyser(BDFIS bdfis) {
        this.bdfis = bdfis;
        this.numberOfEvaluations = 0;
        this.order = null;
    }

    /**
     *
     * @param permission
     * @param decisionMaker
     * @param handler
     * @param verbose
     */
    abstract public void analyse(String permission, IDecisionMaker decisionMaker, IResultHandler handler, boolean verbose);

    public void resetAnalyser() {
        this.numberOfEvaluations = 0;
    }

    public int getNumberOfEvaluations() {
        return numberOfEvaluations;
    }
    
    public void setVariableOrdering(List<String> order) {
        this.order = order;
    }
    
    public List<String> getVariableOrdering() {
        return Collections.unmodifiableList(this.order);
    }
}
