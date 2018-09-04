/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.dfcl2.test;

import it.av.fac.dfcl2.DefuzzifyEIOMS;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import net.sourceforge.jFuzzyLogic.rule.Variable;

/**
 *
 * @author Diogo Regateiro <diogoregateiro@ua.pt>
 */
public class ORESAccessControlInferenceService implements DefuzzifyEIOMS {

    private final String variableLabel;

    public ORESAccessControlInferenceService(String variableLabel) {
        this.variableLabel = variableLabel;
    }

    @Override
    public double process(Collection<Variable> inputVector) {
        double output = 0.0;

        for(Variable var : inputVector) {
            if(var.getName().equals("Edit")) {
                double granted = var.getMembershipFunction("Granted").getParameter(1);
                double denied = var.getMembershipFunction("Denied").getParameter(1);
                output =  granted / (granted + denied);
            }
        }
        
        return output;
    }

    @Override
    public String getOutputVariableLabel() {
        return variableLabel;
    }
}
