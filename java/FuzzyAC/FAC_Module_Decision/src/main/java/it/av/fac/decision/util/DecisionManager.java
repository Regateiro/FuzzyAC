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
public class DecisionManager {

    private Map<Integer, Decision> lastDecisions;
    private Map<Integer, Decision> currentDecisions;

    public DecisionManager() {
        this.lastDecisions = new HashMap<>();
        this.currentDecisions = new HashMap<>();
    }

    public boolean isLastDecisionApplicable(Contribution lastChangeContribution, int x) {
        if (this.lastDecisions.containsKey(x)) {
            Decision lastDecision = this.lastDecisions.get(x);
            
            if(lastChangeContribution == Contribution.NONE) {
                return true;
            }

            if (lastDecision == Decision.Granted && lastChangeContribution == Contribution.GRANT) {
                return true;
            }

            if (lastDecision == Decision.Denied && lastChangeContribution == Contribution.DENY) {
                return true;
            }
        }
        return false;
    }

    public Decision useLastDecision(int x) {
        Decision decision = this.lastDecisions.get(x);
        saveDecision(x, decision);
        return decision;
    }

    public void ageDecisions() {
        this.lastDecisions.clear();
        this.lastDecisions = this.currentDecisions;
        this.currentDecisions = new HashMap<>();
    }

    public void saveDecision(int currentValue, Decision decision) {
        this.currentDecisions.put(currentValue, decision);
    }

    public void clear() {
        this.currentDecisions.clear();
        this.lastDecisions.clear();
    }

}
