/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.decision.util.decision;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + Objects.hashCode(this.decision);
        hash = 97 * hash + Objects.hashCode(this.variables);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DecisionResult other = (DecisionResult) obj;
        if (this.decision != other.decision) {
            return false;
        }
        return Objects.equals(this.variables, other.variables);
    }

     
}
