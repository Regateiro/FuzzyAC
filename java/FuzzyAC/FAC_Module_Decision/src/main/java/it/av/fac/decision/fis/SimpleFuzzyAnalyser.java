/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.decision.fis;

import static it.av.fac.decision.fis.FuzzyEvaluator.FB_VARIABLE_INFERENCE_PHASE_NAME;
import it.av.fac.decision.util.Decision;
import it.av.fac.decision.util.DecisionResult;
import it.av.fac.decision.util.MultiRangeValue;
import it.av.fac.decision.util.RangeValue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.sourceforge.jFuzzyLogic.membership.MembershipFunction;
import net.sourceforge.jFuzzyLogic.rule.LinguisticTerm;
import net.sourceforge.jFuzzyLogic.rule.Variable;

/**
 *
 * @author Diogo Regateiro
 */
public class SimpleFuzzyAnalyser extends AbstractFuzzyAnalyser {

    public SimpleFuzzyAnalyser(FuzzyEvaluator feval) {
        super(feval);
    }

    /**
     * 
     * @param permission Doesn't matter.
     * @return 
     */
    @Override
    public List<DecisionResult> analyse(String permission) {
        this.permissionToAnalyse = permission;
        
        //reset the analyser
        resetAnalyser();
        
        //List of input variable names
        List<String> inputVars = new ArrayList<>();

        //Get the list of variables used in the VariableInference function block.
        Collection<Variable> variables = feval.getFis().getFunctionBlock(FB_VARIABLE_INFERENCE_PHASE_NAME).variables();

        //Retrieves the variables for the VariableInference function block, filtering for only input variables and adds their name to the inputVars list.
        variables.stream().filter((var) -> var.isInput()).forEach((var) -> inputVars.add(var.getName()));

        //Obtains the edge conditions.
        return findEdgeIntegerConditions(0.5, inputVars);
    }

    /**
     * Finds the input values for which the output weight for the permissions
     * equals alphaCut.
     *
     * @param alphaCut The value to check the permission weights for equality.
     * @param variables The name of the variables for the FIS.
     */
    private List<DecisionResult> findEdgeIntegerConditions(double alphaCut, List<String> variables) {
        List<MultiRangeValue> variableMap = new ArrayList<>();
        for (int i = 0; i < variables.size(); i++) {
            //get variable name
            String varName = variables.get(i);

            //add temporary solution
            variableMap.add(new MultiRangeValue(Arrays.asList(new RangeValue(varName, (int) minXValue(varName), (int) maxXValue(varName)))));
        }
        
        variableMap.add(variableMap.stream().filter((var) -> var.getVarName().equalsIgnoreCase("Number_Of_Citations")).findFirst().get());
        variableMap.add(variableMap.stream().filter((var) -> var.getVarName().equalsIgnoreCase("Days_Since_Last_Publication")).findFirst().get());
        variableMap.add(variableMap.stream().filter((var) -> var.getVarName().equalsIgnoreCase("Number_Of_Publications")).findFirst().get());
        variableMap.remove(0);
        variableMap.remove(0);
        variableMap.remove(0);
        
        // recursive function call
        return findEdgeIntegerConditionsRec(alphaCut, variableMap, 0);
    }
    
    protected List<DecisionResult> findEdgeIntegerConditionsRec(double alphaCut, List<MultiRangeValue> variableMap, int varIdx) {
        List<DecisionResult> results = new ArrayList<>();
        
        if (varIdx < variableMap.size()) {
            do {
                //recursive call to add the other variable to the list
                results.addAll(findEdgeIntegerConditionsRec(alphaCut, variableMap, varIdx + 1));

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
            numberOfEvaluations++;

            try {
                //Adds the variables that resulted on the provided alphaCut
                Decision decision = (evaluation.get(permissionToAnalyse).getValue() > alphaCut ? Decision.Granted : Decision.Denied);
                DecisionResult result = new DecisionResult(decision, variablesToEvaluate);

                if (lastResult != null) {
                    if (!lastResult.decisionMatches(result)) {
                        results.add(lastResult);
                        results.add(result);
                    }
                }

                lastResult = result;
            } catch (NullPointerException ex) {
                System.err.println("[SimpleFuzzyAnalyser] : Null pointer exception, it's possible that the permission " + permissionToAnalyse + " is not defined.");
            }
        }
        
        return results;
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
}
