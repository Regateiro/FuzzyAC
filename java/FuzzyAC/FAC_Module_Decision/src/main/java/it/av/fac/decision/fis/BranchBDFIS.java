/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.decision.fis;

import java.util.HashMap;
import java.util.Map;
import net.sourceforge.jFuzzyLogic.FunctionBlock;
import net.sourceforge.jFuzzyLogic.plot.JFuzzyChart;
import net.sourceforge.jFuzzyLogic.rule.Variable;
import org.antlr.runtime.RecognitionException;

/**
 * Fuzzy Inference System BDFISFuzzyEvaluator
 *
 * @author Diogo Regateiro
 */
public class BranchBDFIS extends BDFIS {

    public BranchBDFIS(String fcl, boolean inlineFcl) throws RecognitionException {
        super(fcl, inlineFcl);
    }

    @Override
    public Map<String, Variable> evaluate(Map<String, Double> inVariables, boolean debug) {
        Map<String, Variable> ret = new HashMap<>();

        // Error while loading?
        if (fis == null) {
            System.err.println("Not initialized.");
            return null;
        }

        // Print ruleSet
        if (debug) {
            System.out.println(fis);
        }

        FunctionBlock vifb = fis.getFunctionBlock(FB_VARIABLE_INFERENCE_PHASE_NAME);
        FunctionBlock acfb = fis.getFunctionBlock(FB_ACCESS_CONTROL_PHASE_NAME);
        vifb.reset();
        acfb.reset();

        // Set inputs
        inVariables.keySet().forEach((varName) -> {
            fis.setVariable(vifb.getName(), varName, inVariables.get(varName));
        });

        // Show 
        if (debug) {
            JFuzzyChart.get().chart(vifb);
        }

        // Evaluate
        vifb.evaluate();

        // Save output variables as input for the next functionblock
        vifb.variables().stream().filter((variable) -> (variable.isOutput())).forEach((outVariable) -> {
            if (debug) {
                JFuzzyChart.get().chart(outVariable, outVariable.getDefuzzifier(), true);
            }

            //add the input value for the AccessControl function block
            fis.setVariable(acfb.getName(), outVariable.getName(), outVariable.getLatestDefuzzifiedValue());
        });

        // Show 
        if (debug) {
            JFuzzyChart.get().chart(acfb);
        }

        // Evaluate
        acfb.evaluate();

        acfb.variables().stream().filter((variable) -> (variable.isOutput())).forEach((variable) -> {
            if (debug) {
                JFuzzyChart.get().chart(variable, variable.getDefuzzifier(), true);
            }

            ret.put(variable.getName(), variable);
        });

        return ret;
    }

    public static void main(String[] args) throws Exception {
        Map<String, Double> vars = new HashMap<>();

        String testFile = "academic_branch.fcl";

        switch (testFile) {
            case "academic_branch.fcl": // works
                vars.put("Number_Of_Publications", 12.0);
                vars.put("Number_Of_Citations", 50.0);
        }

        BranchBDFIS feval = new BranchBDFIS(testFile, false);
        System.out.println(feval.evaluate(vars, true));
    }
}
