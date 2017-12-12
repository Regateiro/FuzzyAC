/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.decision.handlers;

import java.util.HashMap;
import java.util.Map;
import net.sourceforge.jFuzzyLogic.FunctionBlock;
import net.sourceforge.jFuzzyLogic.membership.MembershipFunctionSingleton;
import net.sourceforge.jFuzzyLogic.membership.Value;

/**
 * Custom Membership Function
 *
 * @author Diogo Regateiro
 */
public abstract class CustomMF {

    /**
     * Generate all membership degrees given the input for this function.
     *
     * @return The membership degrees of the linguistic terms this custom
     * function handles for each input variable.
     */
    protected abstract Map<String, Map<String, Double>> processInternal();

    /**
     * Generate all membership degrees given the input for this function. It
     * automatically prepares the FIS to use the generated membership degrees.
     *
     * @param fb The function block of the FIS where the membership degrees are
     * to be used.
     * @return The membership degrees of the linguistic terms this custom
     * function handles.
     */
    public final Map<String, Double> process(FunctionBlock fb) {
        Map<String, Map<String, Double>> ret = processInternal();
        return mapToMembershipFunctions(fb, ret);
    }

    private Map<String, Double> mapToMembershipFunctions(FunctionBlock fb, Map<String, Map<String, Double>> customVarLTs) {
        // For each input variable in the function block
        fb.getVariables().values().stream().filter((variable) -> (variable.isInput())).forEach((inVariable) -> 
            // Find any linguistic term that matches in the customVarLTs
            customVarLTs.keySet().forEach((varName) -> 
                customVarLTs.get(varName).keySet().stream()
                        .filter((ltName) -> inVariable.getLinguisticTerms().containsKey(ltName))
                        .forEach((ltName) -> 
                            // Set a new membership function that returns the generated value for the linguistic term when given the input value 0.
                            inVariable.getLinguisticTerm(ltName).setMembershipFunction(
                                    new MembershipFunctionSingleton(
                                            Value.ZERO, 
                                            new Value(customVarLTs.get(varName).get(ltName))
                                    )
                            )
                        )
            )
        );

        // Set the input value for each variable to 0.0 to match the linguistic term x value used.
        Map<String, Double> ret = new HashMap<>();
        customVarLTs.keySet().forEach((varName) -> ret.put(varName, 0.0));
        return ret;
    }
}
