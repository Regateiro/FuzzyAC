/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.dfcl2.test;

import it.av.fac.dfcl2.NoDefuzzifyEIOMS;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import net.sourceforge.jFuzzyLogic.rule.Variable;

/**
 *
 * @author Diogo Regateiro <diogoregateiro@ua.pt>
 */
public class ORESInferenceService implements NoDefuzzifyEIOMS {
    private final String variableLabel;

    public ORESInferenceService(String variableLabel) {
        this.variableLabel = variableLabel;
    }
    
    @Override
    public Map<String, Double> process(Collection<Variable> inputVector) {
        Map<String, Double> outputVector = new HashMap<>();
        outputVector.put("Denied", 0.0);
        outputVector.put("Granted", 0.0);
        
        inputVector.parallelStream().filter(var -> var.getName().equals("Revision")).forEach(revision -> {
            //RULE 0: IF (Rev_OK IS True) THEN Edit IS Granted;
            outputVector.put("Granted", Math.max(outputVector.get("Granted"), revision.getMembership("OK")));
            
            //RULE 1: IF (Rev_Spam IS True) AND (Rev_Vandalism IS True) THEN Edit IS Denied;
            outputVector.put("Denied", Math.max(outputVector.get("Denied"), Math.min(
                    revision.getMembership("Spam"), 
                    revision.getMembership("Vandalism") 
            )));
            
            //RULE 2: IF (Rev_Damaging IS True) THEN Edit IS Denied;
            outputVector.put("Denied", Math.max(outputVector.get("Denied"), revision.getMembership("Damaging")));
            
            //RULE 3: IF (Rev_Attack IS True) AND (Rev_Damaging IS True) THEN Edit IS Denied;
            outputVector.put("Denied", Math.max(outputVector.get("Denied"), Math.min(
                    revision.getMembership("Attack"), 
                    revision.getMembership("Damaging") 
            )));
            
            //RULE 4: IF (Rev_Attack IS True) AND (Rev_Vandalism IS True) THEN Edit IS Denied;
            outputVector.put("Denied", Math.max(outputVector.get("Denied"), Math.min(
                    revision.getMembership("Attack"), 
                    revision.getMembership("Vandalism") 
            )));
            
            //RULE 5: IF (Rev_Attack IS True) THEN Edit IS Denied;
            outputVector.put("Denied", Math.max(outputVector.get("Denied"), revision.getMembership("Attack")));
        });
        
        return outputVector;
    }

    @Override
    public String getOutputVariableLabel() {
        return variableLabel;
    }
    
}
