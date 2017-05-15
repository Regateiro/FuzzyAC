package it.av.fac.decision.fis;

import static it.av.fac.decision.fis.FuzzyEvaluator.FB_VARIABLE_INFERENCE_PHASE_NAME;
import it.av.fac.decision.util.SlopeType;
import it.av.fac.decision.util.MultiRangeValue;
import it.av.fac.decision.util.RangeValue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import net.sourceforge.jFuzzyLogic.membership.MembershipFunctionPieceWiseLinear;
import net.sourceforge.jFuzzyLogic.rule.Variable;

/**
 *
 * @author Diogo Regateiro
 */
public class OptimizedFuzzyAnalyser extends AbstractFuzzyAnalyser {

    private final VariableDependenceAnalyser vda;

    public OptimizedFuzzyAnalyser(FuzzyEvaluator feval) {
        super(feval);
        this.vda = new VariableDependenceAnalyser(feval.getFis());
    }

    @Override
    public void analyse(String permission) {
        this.permissionToAnalyse = permission;
        
        //Analyse the variable dependences.
        this.vda.analyse();

        //List of input variable names
        List<String> inputVars = new ArrayList<>();

        //Get the list of variables used in the VariableInference function block.
        Collection<Variable> variables = feval.getFis().getFunctionBlock(FB_VARIABLE_INFERENCE_PHASE_NAME).variables();

        //Retrieves the variables for the VariableInference function block, filtering for only input variables and adds their name to the inputVars list.
        variables.stream().filter((var) -> var.isInput()).forEach((var) -> inputVars.add(var.getName()));

        //Obtains the edge conditions.
        findEdgeIntegerConditions(permission, 0.5, inputVars);

        System.out.println("[OptimizedFuzzyAnalyser] - Number of permission changes found: " + this.numResults.get(permissionToAnalyse).get());
    }

    /**
     * Finds the input values for which the output weight for the permissions
     * equals alphaCut.
     *
     * @param alphaCut The value to check the permission weights for equality.
     * @param variables The name of the variables for the FIS.
     */
    private void findEdgeIntegerConditions(String permission, double alphaCut, List<String> variables) {
        List<MultiRangeValue> variableMap = new ArrayList<>();
        for (int i = 0; i < variables.size(); i++) {
            //get variable name
            String varName = variables.get(i);

            //add temporary solution
            variableMap.add(diffNotZeroRanges(permission, varName));
        }

        // recursive function call
        findEdgeIntegerConditionsRec(alphaCut, variableMap, 0);
    }
    
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
            evaluation.keySet().stream().filter((permission) -> permission.equalsIgnoreCase(permissionToAnalyse)).forEach((permission) -> {
                String decision = (evaluation.get(permission).getValue() > alphaCut ? "Granted" : "Denied");
                String line = String.format("%s : %s [%s]", decision, permission, variablesToEvaluate);

                if (outputBuffer.containsKey(permission)) {
                    if (outputBuffer.get(permission).charAt(0) != line.charAt(0)) {
                        this.numResults.putIfAbsent(permission, new AtomicInteger());
                        this.numResults.get(permission).incrementAndGet();
//                        System.out.println(outputBuffer.get(permission));
//                        System.out.println(line);
//                        System.out.println();
                    }
                }

                outputBuffer.put(permission, line);
            });
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

                // If there is a slope
                if (y1 != y2 && x1 != x2) {
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
}
