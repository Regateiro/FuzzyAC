/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.decision.fis;

import it.av.fac.decision.util.MultiRangeValue;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import net.sourceforge.jFuzzyLogic.rule.Variable;

/**
 *
 * @author DiogoJosé
 */
public abstract class FuzzyAnalyser {

    protected final FuzzyEvaluator feval;
    protected final Map<String, String> outputBuffer;
    protected final AtomicInteger numResults;

    protected FuzzyAnalyser(FuzzyEvaluator feval) {
        this.feval = feval;
        this.outputBuffer = new HashMap<>();
        this.numResults = new AtomicInteger();
    }
    
    abstract public void analyse();
    
    protected void findEdgeIntegerConditionsRec(double alphaCut, List<MultiRangeValue> variableMap, int varIdx) {
        if (varIdx < variableMap.size()) {
            do {
                //recursive call to add the other variable to the list
                findEdgeIntegerConditionsRec(alphaCut, variableMap, varIdx + 1);

                //TODO: Optimize. Breaks the recursive call when the variable is on the range edge
                if (variableMap.get(varIdx).isOnTheEdge()) {
                    variableMap.get(varIdx).invertDirection();
                    break;
                }

                //Searched the rest of the variable, time to update this one by step.
                variableMap.get(varIdx).next();
            } while (true);
        } else {
            //Edge case where there are no more variables.

            //Create evaluation variable map
            Map<String, Double> variablesToEvaluate = new HashMap<>();
            variableMap.stream().forEach((var) -> {
                variablesToEvaluate.put(var.getVarName(), (double) var.getCurrentValue());
            });

            //Evaluates the result using the current variable values.
            Map<String, Variable> evaluation = feval.evaluate(variablesToEvaluate, false);

            //Adds the variables that resulted on the provided alphaCut
            evaluation.keySet().stream().forEach((permission) -> {
                String decision = (evaluation.get(permission).getValue() > alphaCut ? "Granted" : "Denied");
                String line = String.format("%s : %s [%s]", decision, permission, variablesToEvaluate);

                if (outputBuffer.containsKey(permission)) {
                    if (outputBuffer.get(permission).charAt(0) != line.charAt(0)) {
                        this.numResults.incrementAndGet();
//                        System.out.println(outputBuffer.get(permission));
//                        System.out.println(line);
//                        System.out.println();
                    }
                }

                outputBuffer.put(permission, line);
            });
        }
    }
}
