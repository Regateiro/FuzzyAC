package it.av.fac.decision.fis;

import static it.av.fac.decision.fis.FuzzyEvaluator.FB_VARIABLE_INFERENCE_PHASE_NAME;
import it.av.fac.decision.util.MultiRangeValue;
import it.av.fac.decision.util.RangeValue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.sourceforge.jFuzzyLogic.membership.MembershipFunctionPieceWiseLinear;
import net.sourceforge.jFuzzyLogic.rule.Variable;

/**
 *
 * @author Diogo Regateiro
 */
public class OptimizedFuzzyAnalyser extends FuzzyAnalyser {

    private final VariableDependenceAnalyser vda;
    private final Map<String, List<MultiRangeValue>> rangesPerPermission;

    public OptimizedFuzzyAnalyser(FuzzyEvaluator feval) {
        super(feval);
        this.vda = new VariableDependenceAnalyser(feval.getFis());
        this.rangesPerPermission = new HashMap<>();
    }

    @Override
    public void analyse(String permission) {
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

        System.out.println("Number of permission changes founds: " + this.numResults.get());
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
                    ranges.get(linguisticTerm).add(new RangeValue(varName, linguisticTerm, (y2 - y1) / (x2 - x1), (x2 * y1 - x1 * y2) / (x2 - x1), (int) x1, (int) x2));
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

        for (int i = 0; i < finalList.size() - 1;) {
            RangeValue rv1 = finalList.get(i);
            RangeValue rv2 = finalList.get(i + 1);

            if (rv1.overlapsWith(rv2)) {
                finalList.addAll(rv1.splitFrom(rv2));
                if (rv1.getMin() >= rv1.getMax()) {
                    finalList.remove(rv1);
                } else if (rv2.getMin() >= rv2.getMax()) {
                    finalList.remove(rv2);
                }
                
                Collections.sort(finalList, (rv1t, rv2t) -> {
                    return Integer.compare(rv1t.getCurrentValue(), rv2t.getCurrentValue());
                });
            } else {
                i++;
            }
        }

        //sorts the list of ranges
        Collections.sort(finalList, (rv1, rv2) -> {
            return Integer.compare(rv1.getCurrentValue(), rv2.getCurrentValue());
        });

        return new MultiRangeValue(finalList);
    }
}
