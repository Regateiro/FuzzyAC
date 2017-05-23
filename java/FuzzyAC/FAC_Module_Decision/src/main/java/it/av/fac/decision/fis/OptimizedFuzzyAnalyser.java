package it.av.fac.decision.fis;

import static it.av.fac.decision.fis.FuzzyEvaluator.FB_VARIABLE_INFERENCE_PHASE_NAME;
import it.av.fac.decision.util.variables.Contribution;
import it.av.fac.decision.util.decision.Decision;
import it.av.fac.decision.util.decision.DecisionManager;
import it.av.fac.decision.util.decision.DecisionResult;
import it.av.fac.decision.util.decision.IDecisionMaker;
import it.av.fac.decision.util.variables.SlopeType;
import it.av.fac.decision.util.variables.MultiRangeValue;
import it.av.fac.decision.util.variables.RangeValue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.sourceforge.jFuzzyLogic.membership.MembershipFunctionPieceWiseLinear;
import net.sourceforge.jFuzzyLogic.rule.Variable;
import it.av.fac.decision.util.handlers.IResultHandler;

/**
 *
 * @author Diogo Regateiro
 */
public class OptimizedFuzzyAnalyser extends AbstractFuzzyAnalyser {

    private final VariableDependenceAnalyser vda;
    private Contribution lastChangeContribution;
    private final DecisionManager decisionManager;
    private final List<List<DecisionResult>> variableOutputs;
    private final Map<Integer, Integer> lastPassCount;

    public OptimizedFuzzyAnalyser(FuzzyEvaluator feval) {
        super(feval);
        this.vda = new VariableDependenceAnalyser(feval.getFis());
        this.lastChangeContribution = Contribution.UNKNOWN;
        this.decisionManager = new DecisionManager();
        this.variableOutputs = new ArrayList<>();
        this.lastPassCount = new HashMap<>();
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

        //Analyse the variable dependences.
        this.vda.analyse(permission);

        //List of input variable names
        List<String> inputVars = new ArrayList<>();

        //Get the list of variables used in the VariableInference function block.
        Collection<Variable> variables = feval.getFis().getFunctionBlock(FB_VARIABLE_INFERENCE_PHASE_NAME).variables();

        //Retrieves the variables for the VariableInference function block, filtering for only used input variables and adds their name to the inputVars list.
        variables.stream().filter((var) -> var.isInput() && this.vda.variableIsUsed(var.getName())).forEach((var) -> inputVars.add(var.getName()));

        //Obtains the edge conditions.
        findEdgeIntegerConditions(permission, inputVars);
    }

    /**
     * Finds the input values for which the output weight for the permissions
     * equals alphaCut.
     *
     * @param alphaCut The value to check the permission weights for equality.
     * @param variables The name of the variables for the FIS.
     */
    private void findEdgeIntegerConditions(String permission, List<String> variables) {
        List<MultiRangeValue> variableMap = new ArrayList<>();
        for (int i = 0; i < variables.size(); i++) {
            //get variable name
            String varName = variables.get(i);

            //add temporary solution
            variableMap.add(diffNotZeroRanges(permission, varName));
        }

        this.vda.optimizeOrdering(variableMap);

        // recursive function call
        findEdgeIntegerConditionsRec(variableMap, 0, false);
    }

    @SuppressWarnings("empty-statement")
    protected void findEdgeIntegerConditionsRec(List<MultiRangeValue> variableMap, int varIdx, boolean storeResults) {
        if (varIdx < variableMap.size()) {
            variableOutputs.add(new ArrayList<>());

            do {
                if (this.lastChangeContribution != Contribution.NONE) {
                    //update the results count for the new pass
                    if (!storeResults) {
                        for (int i = 0; i < variableOutputs.size(); i++) {
                            this.handler.handleResults(this.variableOutputs.get(i));
                            this.variableOutputs.get(i).clear();
                            this.lastPassCount.put(i, 0);
                        }
                    }

                    lastPassCount.put(varIdx, variableOutputs.get(varIdx).size());

                    //recursive call to add the other variable to the list
                    findEdgeIntegerConditionsRec(variableMap, varIdx + 1, storeResults || variableMap.get(varIdx).getNextValueContribution() == Contribution.NONE);
                } else {
                    if (varIdx == 0) {
                        System.out.print("");
                    }
                    //update the previous results changing only this variable value
                    List<DecisionResult> tempList = new ArrayList<>();

                    //filter only to the results added on the last time the recursive function was called and make a copy of them
                    variableOutputs.get(varIdx)
                            .subList(lastPassCount.get(varIdx), variableOutputs.get(varIdx).size())
                            .stream().forEachOrdered((result) -> tempList.add(result.copy()));

                    //keep only a single value being updated per line
                    Collections.reverse(tempList);

                    //update the copy values for this variable to the current value
                    tempList.parallelStream().forEach((result) -> {
                        result.getVariables().put(variableMap.get(varIdx).getVarName(), (double) variableMap.get(varIdx).getCurrentValue());
                    });

                    //update the results count so only the newly added results will be copied if the contribution remains NONE
                    if (!storeResults) {
                        for (int i = 0; i < variableOutputs.size(); i++) {
                            this.handler.handleResults(this.variableOutputs.get(i));
                            this.variableOutputs.get(i).clear();
                            this.lastPassCount.put(i, 0);
                        }
                    } else {
                        this.lastPassCount.put(varIdx, this.variableOutputs.get(varIdx).size());
                    }

                    //add the new copied results
                    this.variableOutputs.get(varIdx).addAll(tempList);
                }

                //Breaks the recursive call when the variable is on the range edge
                if (variableMap.get(varIdx).isOnTheEdge()) {
                    variableMap.get(varIdx).invertDirection();

                    //if the last variable just finished a pass, save the current ranges.
                    if (varIdx == variableMap.size() - 1) {
                        this.decisionManager.ageDecisions();
                    }

                    //pulls back the results into the parent
                    if (varIdx == 0) {
                        this.handler.handleResults(this.variableOutputs.get(varIdx));
                    } else {
                        this.variableOutputs.get(varIdx - 1).addAll(this.variableOutputs.remove(varIdx));
                    }
                    break;
                }

                //Searched the rest of the variable, time to update this one by step.
                int lastValue = variableMap.get(varIdx).getCurrentValue();
                while (lastValue == variableMap.get(varIdx).next());

                // if the variable updated is not the last one on the list
                if (varIdx < variableMap.size() - 1) {
                    this.lastChangeContribution = variableMap.get(varIdx).getContribution();
                }
            } while (true);
        } else {
            Decision decision = null;

            //Edge case where there are no more variables.
            //Check to see if an evaluation is required
            if (this.lastChangeContribution != Contribution.UNKNOWN) {
                int x = variableMap.get(varIdx - 1).getCurrentValue();
                if (this.decisionManager.isLastDecisionApplicable(this.lastChangeContribution, x)) {
                    decision = this.decisionManager.useLastDecision(x);
                }
            }

            try {
                //Create evaluation variable map
                Map<String, Double> variablesToEvaluate = new HashMap<>();
                variableMap.stream().forEach((var) -> {
                    variablesToEvaluate.put(var.getVarName(), (double) var.getCurrentValue());
                });

                if (decision == null) {
                    //Evaluates the result using the current variable values.
                    Map<String, Variable> evaluation = feval.evaluate(variablesToEvaluate, false);

                    //Adds the variables that resulted on the provided alphaCut
                    decision = decisionMaker.makeDecision(evaluation.get(permissionToAnalyse));
                    numberOfEvaluations++;
                    this.decisionManager.saveDecision(variableMap.get(varIdx - 1).getCurrentValue(), decision);
                }

                DecisionResult result = new DecisionResult(decision, variablesToEvaluate);

                switch (decisionsToReturn) {
                    case ALL:
                        this.variableOutputs.get(varIdx - 1).add(result);
                        break;
                    case ONLY_GRANT:
                        if (result.getDecision() == Decision.Granted) {
                            this.variableOutputs.get(varIdx - 1).add(result);
                        }
                        break;
                    case ONLY_DENY:
                        if (result.getDecision() == Decision.Denied) {
                            this.variableOutputs.get(varIdx - 1).add(result);
                        }
                        break;
                }
            } catch (NullPointerException ex) {
                System.err.println("[OptimizedFuzzyAnalyser] : Null pointer exception, it's possible that the permission " + permissionToAnalyse + " is not defined.");
            }
        }
    }

    /**
     * Determines the x value ranges for which the differential is not 0.
     *
     * @param varName
     * @return
     */
    private MultiRangeValue diffNotZeroRanges(String permission, String varName) {
        Map<String, List<RangeValue>> ranges = new HashMap<>();

        // Get the variable
        Variable variable = feval.getFis().getFunctionBlock(FB_VARIABLE_INFERENCE_PHASE_NAME).getVariable(varName);

        // For each LT, add the defined points to the map of points
        variable.getLinguisticTerms().values().stream().forEachOrdered((lt) -> {
            String linguisticTerm = lt.getTermName();
            MembershipFunctionPieceWiseLinear mf = (MembershipFunctionPieceWiseLinear) lt.getMembershipFunction();
            for (int i = 0; i + 3 < mf.getParametersLength(); i += 2) {
                double x1 = mf.getParameter(i);
                double y1 = mf.getParameter(i + 1);
                double x2 = mf.getParameter(i + 2);
                double y2 = mf.getParameter(i + 3);

                if (x1 != x2) {
                    // Add to the list.
                    ranges.putIfAbsent(linguisticTerm, new ArrayList<>());
                    ranges.get(linguisticTerm).add(new RangeValue(varName, SlopeType.getSlope(y1, y2), (int) x1, (int) x2));
                }
            }
        });

        //TODO: Use the VDA to optimize the ranges to search.
        //add information about wether a range contributes to just one decision and allow skipping
        //the merging will have to be modified due to this as verified.
        //Transform the list of range values into a non-overlapping multirangevalue
        MultiRangeValue ret = processRangeValueList(ranges, permission, varName);

        return ret;
    }

    /**
     *
     * @param rangesPerLT Ranges separated by linguistic term.
     * @param permission The permission to process the ranges for.
     * @param varName The variable name.
     * @return
     */
    public MultiRangeValue processRangeValueList(Map<String, List<RangeValue>> rangesPerLT, String permission, String varName) {
        List<RangeValue> finalList = new ArrayList<>();

        //process the contribution of each RangedValue
        rangesPerLT.keySet().parallelStream().forEach((termName) -> {
            List<RangeValue> ranges = rangesPerLT.get(termName);
            boolean contributesToGrant = vda.contributesOnlyToGrant(permission, varName, termName);
            boolean contributesToDeny = vda.contributesOnlyToDeny(permission, varName, termName);

            ranges.parallelStream().forEach((range) -> {
                range.setContribution(contributesToGrant, contributesToDeny);
            });
        });

        //consolidates the ranges from every LT
        rangesPerLT.keySet().stream().forEach((termName) -> {
            finalList.addAll(rangesPerLT.get(termName));
        });

        //sorts the list of ranges
        Collections.sort(finalList, (rv1, rv2) -> {
            return Integer.compare(rv1.getCurrentValue(), rv2.getCurrentValue());
        });

        //clear overlapping ranges
        for (int i = 0; i < finalList.size() - 1;) {
            RangeValue rv1 = finalList.get(i);
            RangeValue rv2 = finalList.get(i + 1);

            // if ranges overlap
            if (rv1.overlapsWith(rv2)) {
                // add all ranges created from resolving the overlap.
                finalList.addAll(rv1.splitFrom(rv2));

                // remove the overlapping ranges.
                finalList.remove(rv1);
                finalList.remove(rv2);

                // sort and then retest.
                Collections.sort(finalList, (rv1t, rv2t) -> {
                    return Integer.compare(rv1t.getCurrentValue(), rv2t.getCurrentValue());
                });
            } else {
                // ranges do not overlap, continue to the next two ranges.
                i++;
            }
        }

        return new MultiRangeValue(finalList);
    }

    @Override
    public void resetAnalyser() {
        super.resetAnalyser();
        this.decisionManager.clear();
        this.lastPassCount.clear();
        this.variableOutputs.clear();
        this.lastChangeContribution = Contribution.UNKNOWN;
    }
}
