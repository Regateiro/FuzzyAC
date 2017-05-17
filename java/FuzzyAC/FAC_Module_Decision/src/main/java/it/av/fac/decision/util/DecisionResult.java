/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.decision.util;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Diogo Regateiro
 */
public class DecisionResult {

    private final Decision decision;
    private final Map<String, Double> variables;

    public DecisionResult(Decision decision, Map<String, Double> variables) {
        this.decision = decision;
        this.variables = variables;
    }

    public Decision getDecision() {
        return decision;
    }

    public Map<String, Double> getVariables() {
        return variables;
    }

    public boolean decisionMatches(DecisionResult dr) {
        return this.getDecision() == dr.getDecision();
    }

    @Override
    public String toString() {
        return String.format("%s : %s", decision, variables);
    }

    public DecisionResult copy() {
        return new DecisionResult(decision, new HashMap<>(variables));
    }
}
