/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.decision.fis;

import static it.av.fac.decision.fis.FuzzyEvaluator.FB_VARIABLE_INFERENCE_PHASE_NAME;
import it.av.fac.decision.util.MultiRangeValue;
import it.av.fac.decision.util.RangeValue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import net.sourceforge.jFuzzyLogic.membership.MembershipFunctionPieceWiseLinear;
import net.sourceforge.jFuzzyLogic.rule.Variable;

/**
 *
 * @author Diogo Regateiro
 */
public class OptimizedFuzzyAnalyser extends FuzzyAnalyser {
    
    private final VariableDependenceAnalyser vda;

    public OptimizedFuzzyAnalyser(FuzzyEvaluator feval) {
        super(feval);
        this.vda = new VariableDependenceAnalyser(feval.getFis());
    }

    @Override
    public void analyse() {
        //Analyse the variable dependences.
        this.vda.analyse();
        
        //List of input variable names
        List<String> inputVars = new ArrayList<>();

        //Get the list of variables used in the VariableInference function block.
        Collection<Variable> variables = feval.getFis().getFunctionBlock(FB_VARIABLE_INFERENCE_PHASE_NAME).variables();

        //Retrieves the variables for the VariableInference function block, filtering for only input variables and adds their name to the inputVars list.
        variables.stream().filter((var) -> var.isInput()).forEach((var) -> inputVars.add(var.getName()));

        //Obtains the edge conditions.
        findEdgeIntegerConditions(0.5, inputVars);

        System.out.println("Number of permission changes founds: " + this.numResults.get());
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
            variableMap.add(new MultiRangeValue(diffNotZeroRanges(varName)));
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
    private List<RangeValue> diffNotZeroRanges(String varName) {
        List<RangeValue> ranges = new ArrayList<>();

        // Get the variable
        Variable variable = feval.getFis().getFunctionBlock(FB_VARIABLE_INFERENCE_PHASE_NAME).getVariable(varName);

        // For each LT, add the defined points to the map of points
        variable.getLinguisticTerms().values().stream().forEachOrdered((lt) -> {
            MembershipFunctionPieceWiseLinear mf = (MembershipFunctionPieceWiseLinear) lt.getMembershipFunction();
            for (int i = 0; i + 3 < mf.getParametersLength(); i += 2) {
                double x1 = mf.getParameter(i);
                double y1 = mf.getParameter(i + 1);
                double x2 = mf.getParameter(i + 2);
                double y2 = mf.getParameter(i + 3);

                // If there is a slope
                if (y1 != y2 && x1 != x2) {
                    // Add to the list.
                    ranges.add(new RangeValue(varName, lt.getTermName(), (y2 - y1) / (x2 - x1), (int) x1, (int) x2));
                }
            }
        });
        
        //TODO: Use the VDA to optimize the ranges to search.
        //add information about wether a range contributes to just one decision and allow skipping
        //the merging will have to be modified due to this as verified.

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

        Collections.sort(ranges, (rv1, rv2) -> {
            return Integer.compare(rv1.getCurrentValue(), rv2.getCurrentValue());
        });

        return ranges;
    }
}
