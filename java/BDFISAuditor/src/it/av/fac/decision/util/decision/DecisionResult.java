/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.decision.util.decision;

import it.av.fac.decision.util.variables.Contribution;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 *
 * @author Diogo Regateiro <diogoregateiro@ua.pt>
 */
public class DecisionResult {

    private Decision decision;
    private final Map<String, Double> variables;
    private final int changedVarIdx;
    private Contribution lastChangedContribution;

    public DecisionResult(Decision decision, Map<String, Double> variables) {
        this.decision = decision;
        this.variables = variables;
        this.changedVarIdx = -1;
        this.lastChangedContribution = Contribution.UNKNOWN;
    }

    public DecisionResult(Decision decision, Map<String, Double> variables, int changedVarIdx, Contribution lastChangedContribution) {
        this.decision = decision;
        this.variables = variables;
        this.changedVarIdx = changedVarIdx;
        this.lastChangedContribution = lastChangedContribution;
    }

    public Decision getDecision() {
        return decision;
    }
    
    public void setDecision(Decision decision) {
        this.decision = decision;
    }

    public int getChangedVarIdx() {
        return changedVarIdx;
    }

    public Contribution getLastChangedContribution() {
        return lastChangedContribution;
    }

    public void setLastChangedContribution(Contribution lastChangedContribution) {
        this.lastChangedContribution = lastChangedContribution;
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
        return new DecisionResult(decision, new HashMap<>(variables), this.changedVarIdx, this.lastChangedContribution);
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
