/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.decision.fis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.sourceforge.jFuzzyLogic.membership.MembershipFunction;
import net.sourceforge.jFuzzyLogic.rule.Variable;

/**
 *
 * @author Diogo Regateiro
 */
public class FuzzyAnalyser {

    private final static String FB_VARIABLE_INFERENCE_PHASE_NAME = "VariableInference";
    private final static String FB_ACCESS_CONTROL_PHASE_NAME = "AccessControl";
    private final FuzzyEvaluator feval;
    private final Map<String, String> outputBuffer;

    public FuzzyAnalyser(FuzzyEvaluator feval) {
        this.feval = feval;
        this.outputBuffer = new HashMap<>();
    }

    public void analyse() {
        //List of input variable names
        List<String> inputVars = new ArrayList<>();

        //Get the list of variables used in the VariableInference function block.
        Collection<Variable> variables = feval.getFis().getFunctionBlock(FB_VARIABLE_INFERENCE_PHASE_NAME).variables();

        //Retrieves the variables for the VariableInference function block, filtering for only input variables and adds their name to the inputVars list.
        variables.stream().filter((var) -> var.isInput()).forEach((var) -> inputVars.add(var.getName()));

        //Obtains the edge conditions.
        findEdgeIntegerConditions(0.5, inputVars);
    }

    /**
     * Finds the input values for which the output weight for the permissions
     * equals alphaCut.
     *
     * @param alphaCut The value to check the permission weights for equality.
     * @param variables The name of the variables for the FIS.
     */
    private void findEdgeIntegerConditions(double alphaCut, List<String> variables) {
        List<RangeValue> variableMap = new ArrayList<>();
        for (int i = 0; i < variables.size(); i++) {
            //get variable name
            String varName = variables.get(i);

            //get the minimum X value
            double varValue = minXValue(varName);

            //add temporary solution
            variableMap.add(new RangeValue(varName, (int) minXValue(varName), (int) maxXValue(varName)));
        }

        // recursive function call
        findEdgeIntegerConditionsRec(alphaCut, variableMap, 0);
    }

    private void findEdgeIntegerConditionsRec(double alphaCut, List<RangeValue> variableMap, int varIdx) {
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
                variablesToEvaluate.put(var.getName(), (double) var.getCurrentValue());
            });

            //Evaluates the result using the current variable values.
            Map<String, Variable> evaluation = feval.evaluate(variablesToEvaluate, false);

            //Adds the variables that resulted on the provided alphaCut
            evaluation.keySet().stream().forEach((permission) -> {
                String decision = (evaluation.get(permission).getValue() > alphaCut ? "Granted" : "Denied");
                String line = String.format("%s : %s [%s]", decision, permission, variablesToEvaluate);
                
                if(outputBuffer.containsKey(permission)){
                    if(outputBuffer.get(permission).charAt(0) != line.charAt(0)) {
                        System.out.println(outputBuffer.get(permission));
                        System.out.println(line);
                        System.out.println();
                    }
                }
                
                outputBuffer.put(permission, line);
            });
        }
    }

    private double maxXValue(String varName) {
        Variable variable = feval.getFis().getFunctionBlock(FB_VARIABLE_INFERENCE_PHASE_NAME).getVariable(varName);

        double xmax = 0.0;
        for (net.sourceforge.jFuzzyLogic.rule.LinguisticTerm lt : variable.getLinguisticTerms().values()) {
            MembershipFunction mf = lt.getMembershipFunction();
            for (int i = 0; i < mf.getParametersLength(); i += 2) {
                double x = mf.getParameter(i);
                if (x > xmax) {
                    xmax = x;
                }
            }
        }

        return xmax;
    }

    private double minXValue(String varName) {
        Variable variable = feval.getFis().getFunctionBlock(FB_VARIABLE_INFERENCE_PHASE_NAME).getVariable(varName);

        double xmin = Integer.MAX_VALUE;
        for (net.sourceforge.jFuzzyLogic.rule.LinguisticTerm lt : variable.getLinguisticTerms().values()) {
            MembershipFunction mf = lt.getMembershipFunction();
            for (int i = 0; i < mf.getParametersLength(); i += 2) {
                double x = mf.getParameter(i);
                if (x < xmin) {
                    xmin = x;
                }
            }
        }

        return xmin;
    }

    //Updates a value between two limits, moving up from the minimum value to the max and then down once the max value is reached.
    //Helps with making sure that only one variable at a tie is updated and only by a step of 1;
    private class RangeValue {

        private final String name;
        private final int max;
        private final int min;
        private int currVal;
        private int direction;

        public RangeValue(String name, int min, int max) {
            this.name = name;
            this.max = max;
            this.min = min;
            this.currVal = min;
            this.direction = 1;
        }

        public int next() {
            if (isOnTheEdge()) {
                invertDirection();
            }
            currVal = currVal + direction;
            return currVal;
        }

        private boolean isOnTheEdge() {
            return currVal + direction < min || currVal + direction > max;
        }

        public String getName() {
            return name;
        }

        public int getCurrentValue() {
            return currVal;
        }

        @Override
        public String toString() {
            return String.format("%d", currVal);
        }

        private void invertDirection() {
            direction *= -1;
        }
    }
}
