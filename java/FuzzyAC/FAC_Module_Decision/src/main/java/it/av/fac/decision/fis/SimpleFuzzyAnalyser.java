/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.decision.fis;

import static it.av.fac.decision.fis.FuzzyEvaluator.FB_VARIABLE_INFERENCE_PHASE_NAME;
import it.av.fac.decision.util.decision.Decision;
import it.av.fac.decision.util.decision.DecisionResult;
import it.av.fac.decision.util.decision.IDecisionMaker;
import it.av.fac.decision.util.handlers.IResultHandler;
import it.av.fac.decision.util.variables.MultiRangeValue;
import it.av.fac.decision.util.variables.RangeValue;
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
     * @param permission
     * @param decisionMaker
     * @param decisionsToResult
     * @param handler
     */
    @Override
    public void analyse(String permission, IDecisionMaker decisionMaker, DecisionResultsToReturn decisionsToResult, IResultHandler handler) {
        this.permissionToAnalyse = permission;
        this.decisionsToReturn = decisionsToResult;
        this.handler = handler;
        this.decisionMaker = decisionMaker;

        //reset the analyser
        resetAnalyser();

        //List of input variable names
        List<String> inputVars = new ArrayList<>();

        //Get the list of variables used in the VariableInference function block.
        Collection<Variable> variables = feval.getFis().getFunctionBlock(FB_VARIABLE_INFERENCE_PHASE_NAME).variables();

        //Retrieves the variables for the VariableInference function block, filtering for only input variables and adds their name to the inputVars list.
        variables.stream().filter((var) -> var.isInput()).forEach((var) -> inputVars.add(var.getName()));

        //Obtains the edge conditions.
        findEdgeIntegerConditions(inputVars);
    }

    /**
     * Finds the input values for which the output weight for the permissions
     * equals alphaCut.
     *
     * @param alphaCut The value to check the permission weights for equality.
     * @param variables The name of the variables for the FIS.
     */
    private void findEdgeIntegerConditions(List<String> variables) {
        List<MultiRangeValue> variableMap = new ArrayList<>();
        for (int i = 0; i < variables.size(); i++) {
            //get variable name
            String varName = variables.get(i);

            //add temporary solution
            variableMap.add(new MultiRangeValue(Arrays.asList(new RangeValue(varName, (int) minXValue(varName), (int) maxXValue(varName)))));
        }
        
        System.out.println(variableMap);

        // recursive function call
        findEdgeIntegerConditionsRec(variableMap, 0);
    }

    protected void findEdgeIntegerConditionsRec(List<MultiRangeValue> variableMap, int varIdx) {
        if (varIdx < variableMap.size()) {
            do {
                //recursive call to add the other variable to the list
                findEdgeIntegerConditionsRec(variableMap, varIdx + 1);

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
                Decision decision = decisionMaker.makeDecision(evaluation.get(permissionToAnalyse));
                DecisionResult result = new DecisionResult(decision, variablesToEvaluate);

                switch (decisionsToReturn) {
                    case ALL:
                        handler.handleSingleResult(result);
                        break;
                    case ONLY_GRANT:
                        if (result.getDecision() == Decision.Granted) {
                            handler.handleSingleResult(result);
                        }
                        break;
                    case ONLY_DENY:
                        if (result.getDecision() == Decision.Denied) {
                            handler.handleSingleResult(result);
                        }
                        break;
                }

            } catch (NullPointerException ex) {
                System.err.println("[SimpleFuzzyAnalyser] : Null pointer exception, it's possible that the permission " + permissionToAnalyse + " is not defined.");
            }
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
}
