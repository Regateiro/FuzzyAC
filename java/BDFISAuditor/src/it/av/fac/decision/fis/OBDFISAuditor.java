package it.av.fac.decision.fis;

import static it.av.fac.decision.fis.BDFIS.FB_VARIABLE_INFERENCE_PHASE_NAME;
import it.av.fac.decision.util.variables.Contribution;
import it.av.fac.decision.util.decision.Decision;
import it.av.fac.decision.util.decision.DecisionResult;
import it.av.fac.decision.util.decision.IDecisionMaker;
import it.av.fac.decision.util.variables.SlopeType;
import it.av.fac.decision.util.variables.MultiRangeValue;
import it.av.fac.decision.util.variables.RangeValue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.sourceforge.jFuzzyLogic.membership.MembershipFunctionPieceWiseLinear;
import net.sourceforge.jFuzzyLogic.rule.Variable;
import it.av.fac.decision.util.handlers.IResultHandler;

/**
 *
 * @author Diogo Regateiro <diogoregateiro@ua.pt>
 */
public class OBDFISAuditor extends AbstractFuzzyAnalyser {

    private final VariableDependenceAnalyser vda;
    private Contribution lastChangeContribution;
    private final List<List<DecisionResult>> variableOutputs;
    private final Map<Integer, Integer> lastPassCount;

    public OBDFISAuditor(BDFIS bdfis) {
        super(bdfis);
        this.vda = new VariableDependenceAnalyser(bdfis.getFIS());
        this.lastChangeContribution = Contribution.UNKNOWN;
        this.variableOutputs = new ArrayList<>();
        this.lastPassCount = new HashMap<>();
        this.vda.analyse();
    }

    /**
     *
     * @param permission
     * @param decisionMaker
     * @param handler
     */
    @Override
    public void analyse(String permission, IDecisionMaker decisionMaker, IResultHandler handler, boolean verbose) {
        this.permissionToAnalyse = permission;
        this.handler = handler;
        this.decisionMaker = decisionMaker;

        //reset the analyser
        resetAnalyser();

        //List of input variable names
        List<String> inputVars = new ArrayList<>();

        //Retrieves the variables for the VariableInference function block, filtering for only used input variables and adds their name to the inputVars list.
        bdfis.getInputVariableNameList(FB_VARIABLE_INFERENCE_PHASE_NAME).stream().filter((varName) -> this.vda.variableIsUsed(varName)).forEach((varName) -> inputVars.add(varName));

        if (order != null) {
            //do ordering
            if (inputVars.containsAll(order) && order.containsAll(inputVars)) {
                order.stream().forEachOrdered((var) -> {
                    inputVars.remove(var);
                    inputVars.add(var);
                });
            } else {
                System.out.println("Variables found do not match ordering received: " + inputVars);
            }
        }

        //Obtains the edge conditions.
        findEdgeIntegerConditions(permission, inputVars, verbose);
    }

    /**
     * Finds the input values for which the output weight for the permissions
     * equals alphaCut.
     *
     * @param alphaCut The value to check the permission weights for equality.
     * @param variables The name of the variables for the FIS.
     */
    private void findEdgeIntegerConditions(String permission, List<String> variables, boolean verbose) {
        List<MultiRangeValue> variableMap = new ArrayList<>();
        for (int i = 0; i < variables.size(); i++) {
            //get variable name
            String varName = variables.get(i);

            //add temporary solution
            variableMap.add(diffNotZeroRanges(permission, varName));
        }

        if (order == null) {
            optimizeOrdering(variableMap);
        }

        if (verbose) {
            System.out.println(variableMap);
        }

        // recursive function call
        findEdgeIntegerConditionsRec(variableMap, 0, false);
    }

    @SuppressWarnings("empty-statement")
    protected void findEdgeIntegerConditionsRec(List<MultiRangeValue> variableMap, int varIdx, boolean storeResults) {
        if (varIdx < variableMap.size()) {
            variableOutputs.add(new ArrayList<>());

            do {
                if (this.lastChangeContribution == Contribution.UNKNOWN) {
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
                    findEdgeIntegerConditionsRec(variableMap, varIdx + 1, storeResults || variableMap.get(varIdx).getNextValueContribution() != Contribution.UNKNOWN);
                } else {
                    //update the previous results changing only this variable value
                    List<DecisionResult> newResults = new ArrayList<>();

                    //filter only to the results added on the last time the recursive function was called and make a copy of them
                    variableOutputs.get(varIdx)
                            .subList(lastPassCount.get(varIdx), variableOutputs.get(varIdx).size())
                            .stream().forEachOrdered((result) -> newResults.add(result.copy()));

                    //keep only a single value being updated per line
                    Collections.reverse(newResults);

                    //update the copy values for this variable to the current value
                    newResults.parallelStream().forEach((result) -> {
                        result.getVariables().put(variableMap.get(varIdx).getVarName(), (double) variableMap.get(varIdx).getCurrentValue());
                    });

                    //if the lastChangeContribution is not NONE, then update the decision where it does not match the contribution.
                    if (this.lastChangeContribution == Contribution.GRANT || this.lastChangeContribution == Contribution.DENY) {
                        //reevaluate decisions that do not match the last change contribution
                        for (int idx = 0; idx < newResults.size(); idx++) {
                            DecisionResult result = newResults.get(idx);
                            if (!result.getDecision().matchesContribution(this.lastChangeContribution)) {
                                newResults.remove(idx);
                                newResults.add(idx, evaluateDecision(result.getVariables()));
                            }
                        }
                    }

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
                    this.variableOutputs.get(varIdx).addAll(newResults);

                    DecisionResult lastEvaluatedResult = this.variableOutputs.get(varIdx).get(this.variableOutputs.get(varIdx).size() - 1);

                    //update the variables
                    for (int idx = varIdx + 1; idx < variableMap.size(); idx++) {
                        double resultVarVal = lastEvaluatedResult.getVariables().get(variableMap.get(idx).getVarName());
                        if (resultVarVal == variableMap.get(idx).getMin()) {
                            variableMap.get(idx).setToMin();
                        } else if (resultVarVal == variableMap.get(idx).getMax()) {
                            variableMap.get(idx).setToMax();
                        }
                    }
                }

                //current variable value has been processed
                //Breaks the recursive call when the variable is on the range edge
                if (variableMap.get(varIdx).isOnTheEdge()) {
                    variableMap.get(varIdx).invertDirection();

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

                //save the last variable updated contribution
                this.lastChangeContribution = variableMap.get(varIdx).getContribution();
            } while (true);
        } else {
            //Edge case where there are no more variables.
            try {
                DecisionResult result = evaluateDecision(variableMap);
                this.variableOutputs.get(varIdx - 1).add(result);
            } catch (NullPointerException ex) {
                System.err.println("[OptimizedFuzzyAnalyser] : Null pointer exception, it's possible that the permission " + permissionToAnalyse + " is not defined.");
            }
        }
    }

    /**
     * Evaluates a decision based on variable values using the FIS.
     *
     * @param variableMap
     * @return
     */
    private DecisionResult evaluateDecision(List<MultiRangeValue> variableMap) {
        //Create evaluation variable map
        Map<String, Double> variablesToEvaluate = new HashMap<>();
        variableMap.stream().forEach((var) -> {
            variablesToEvaluate.put(var.getVarName(), (double) var.getCurrentValue());
        });

        //Evaluates the result using the current variable values.
        Map<String, Variable> evaluation = bdfis.evaluate(variablesToEvaluate, false, false);
        this.numberOfEvaluations++;

        //Adds the variables that resulted on the provided alphaCut
        Decision decision = this.decisionMaker.makeDecision(evaluation.get(this.permissionToAnalyse));

        return new DecisionResult(decision, variablesToEvaluate);
    }

    /**
     * Evaluates a decision based on variable values using the FIS.
     *
     * @param variableMap
     * @return
     */
    private DecisionResult evaluateDecision(Map<String, Double> variableMap) {
        //Evaluates the result using the current variable values.
        Map<String, Variable> evaluation = bdfis.evaluate(variableMap, false, false);
        numberOfEvaluations++;

        //Adds the variables that resulted on the provided alphaCut
        Decision decision = decisionMaker.makeDecision(evaluation.get(permissionToAnalyse));

        return new DecisionResult(decision, variableMap);
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
        Variable variable = bdfis.getFIS().getFunctionBlock(FB_VARIABLE_INFERENCE_PHASE_NAME).getVariable(varName);

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

        //merge neighbor ranges of the same type
        for (int i = 0; i < finalList.size() - 1;) {
            RangeValue rv1 = finalList.get(i);
            RangeValue rv2 = finalList.get(i + 1);

            // if ranges overlap
            if (rv1.canMergeWith(rv2)) {
                // add all ranges created from resolving the overlap.
                finalList.add(rv1.mergeWith(rv2));

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

    /**
     * Optimizes the ordering the of the variables.
     *
     * @param variableMap The list of variable in any order.
     */
    private void optimizeOrdering(List<MultiRangeValue> variableMap) {
        //sort the variable according to the amount of Deny/Grant contribution DESC
        Collections.sort(variableMap, (o1, o2) -> {
            double O1_SINGLE = o1.getContributionRangeSize(Contribution.DENY) + o1.getContributionRangeSize(Contribution.GRANT);
            double O2_SINGLE = o2.getContributionRangeSize(Contribution.DENY) + o2.getContributionRangeSize(Contribution.GRANT);
            double O1_UNKNOWN = o1.getContributionRangeSize(Contribution.UNKNOWN);
            double O2_UNKNOWN = o2.getContributionRangeSize(Contribution.UNKNOWN);

            // If both variable have no single ranges, order them by increasing size of their unknown ranges.
            if (O1_SINGLE == 0 && O2_SINGLE == 0) {
                return Double.compare(O1_UNKNOWN, O2_UNKNOWN);
            }

            // Else, order them by decreasing ratio of single ranges to unknown ranges size.
            double O1_RATIO = O1_SINGLE / O1_UNKNOWN;
            double O2_RATIO = O2_SINGLE / O2_UNKNOWN;

            return Double.compare(O2_RATIO, O1_RATIO);
        });

        order = new ArrayList<>();
        variableMap.stream().forEachOrdered((variable) -> {
            order.add(variable.getVarName());
        });
    }

    @Override
    public void resetAnalyser() {
        super.resetAnalyser();
        this.lastPassCount.clear();
        this.variableOutputs.clear();
        this.lastChangeContribution = Contribution.UNKNOWN;
    }
}
