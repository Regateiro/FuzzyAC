/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.decision.util.decision;

import net.sourceforge.jFuzzyLogic.rule.Variable;

/**
 *
 * @author Diogo Regateiro <diogoregateiro@ua.pt>
 */
public class AlphaCutDecisionMaker implements IDecisionMaker {
    
    private final double alphaCut;

    public AlphaCutDecisionMaker(double alphaCut) {
        this.alphaCut = alphaCut;
    }

    @Override
    public Decision makeDecision(Variable fisOutput) {
        return (fisOutput.getValue() > alphaCut ? Decision.Granted : Decision.Denied);
    }
    
}
