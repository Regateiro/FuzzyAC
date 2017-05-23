/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.decision.util.decision;

import net.sourceforge.jFuzzyLogic.rule.Variable;

/**
 * Makes a decision based on the value associated with the output value from the FIS.
 * @author Diogo Regateiro
 */
public interface IDecisionMaker {
    public Decision makeDecision(Variable fisOutput);
}
