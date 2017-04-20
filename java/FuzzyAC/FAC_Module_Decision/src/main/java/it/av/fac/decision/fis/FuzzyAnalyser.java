/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.decision.fis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.sourceforge.jFuzzyLogic.membership.MembershipFunction;
import net.sourceforge.jFuzzyLogic.membership.MembershipFunctionPieceWiseLinear;
import net.sourceforge.jFuzzyLogic.rule.LinguisticTerm;
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
        List<MultiRangeValue> variableMap = new ArrayList<>();
        for (int i = 0; i < variables.size(); i++) {
            //get variable name
            String varName = variables.get(i);

            //add temporary solution
            //variableMap.add(new MultiRangeValue(Arrays.asList(new RangeValue(varName, (int) minXValue(varName), (int) maxXValue(varName)))));
            variableMap.add(new MultiRangeValue(diffNotZeroRanges(varName)));
        }

        // recursive function call
        findEdgeIntegerConditionsRec(alphaCut, variableMap, 0);
    }

    private void findEdgeIntegerConditionsRec(double alphaCut, List<MultiRangeValue> variableMap, int varIdx) {
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

                if (outputBuffer.containsKey(permission)) {
                    if (outputBuffer.get(permission).charAt(0) != line.charAt(0)) {
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
        for (LinguisticTerm lt : variable.getLinguisticTerms().values()) {
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

    /**
     * Determines the x value ranges for which the differential is not 0.
     *
     * @param varName
     * @return
     */
    private List<RangeValue> diffNotZeroRanges(String varName) {
        List<RangeValue> ranges = new ArrayList<>();

        // Get the variable
        Variable variable = feval.getFis().getFunctionBlock(FB_VARIABLE_INFERENCE_PHASE_NAME).getVariable(varName);

        // For each LT, add the defined points to the map of points
        variable.getLinguisticTerms().values().stream().map((lt) -> (MembershipFunctionPieceWiseLinear) lt.getMembershipFunction()).forEachOrdered((mf) -> {
            for (int i = 0; i + 3 < mf.getParametersLength(); i += 2) {
                int x1 = (int) mf.getParameter(i);
                double y1 = mf.getParameter(i + 1);
                int x2 = (int) mf.getParameter(i + 2);
                double y2 = mf.getParameter(i + 3);

                // If there is a slope
                if (y1 != y2) {
                    // Add to the list.
                    ranges.add(new RangeValue(varName, x1, x2));
                }
            }
        });

        //Determine the list of ranges where the differential of the membership function is not 0
        //For each RangeValue
        for (int i = 0; i < ranges.size() - 1; i++) {
            RangeValue rv1 = ranges.get(i);
            //Find each different RangeValue
            for (int j = i + 1; j < ranges.size();) {
                RangeValue rv2 = ranges.get(j);
                //Try to merge if they overlap
                if (rv1.mergeIfOverlaps(rv2)) {
                    //If they overlap, remove rv2 after the merge.
                    ranges.remove(rv2);
                } else {
                    //Else try the next RangeValue
                    j++;
                }
            }
        }
        
        Collections.sort(ranges, (rv1, rv2) -> {return Integer.compare(rv1.currVal, rv2.currVal);});

        return ranges;
    }

    //Updates a value between two limits, moving up from the minimum value to the max and then down once the max value is reached.
    //Helps with making sure that only one variable at a tie is updated and only by a step of 1;
    private class RangeValue {

        private final String name;
        private int max;
        private int min;
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
            currVal = currVal + direction;
            return currVal;
        }

        public boolean isOnTheEdge() {
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

        public void invertDirection() {
            direction *= -1;
        }

        public boolean mergeIfOverlaps(RangeValue rv) {
            if ((rv.min < this.max && rv.max >= this.max) || (this.min < rv.max && this.max >= rv.max)) {
                this.min = Math.min(rv.min, this.min);
                this.max = Math.max(rv.max, this.max);
                return true;
            }

            return false;
        }
    }

    private class MultiRangeValue {

        private final List<RangeValue> ranges;
        private int idx;
        private int direction;

        public MultiRangeValue(List<RangeValue> ranges) {
            this.ranges = ranges;
            this.idx = 0;
            this.direction = 1;
        }

        public boolean isOnTheEdge() {
            return (idx + direction < 0 || idx + direction == ranges.size()) && ranges.get(idx).isOnTheEdge();
        }

        public void invertDirection() {
            ranges.get(idx).invertDirection();
            direction *= -1;
        }

        public int next() {
            //If the variable is on the edge
            if (ranges.get(idx).isOnTheEdge()) {
                ranges.get(idx).invertDirection();
                
                //Is there another variable after this one?
                if (idx + direction >= 0 && idx + direction < ranges.size()) {
                    // if so move to it.
                    idx += direction;
                }
            }

            return ranges.get(idx).next();
        }

        public String getName() {
            return ranges.get(idx).getName();
        }

        public int getCurrentValue() {
            return ranges.get(idx).getCurrentValue();
        }
    }
}
