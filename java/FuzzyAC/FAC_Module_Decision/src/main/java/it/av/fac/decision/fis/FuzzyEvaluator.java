/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.decision.fis;

import it.av.fac.decision.util.DecisionResult;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sourceforge.jFuzzyLogic.FIS;
import net.sourceforge.jFuzzyLogic.FunctionBlock;
import net.sourceforge.jFuzzyLogic.defuzzifier.DefuzzifierCenterOfGravitySingletons;
import net.sourceforge.jFuzzyLogic.membership.MembershipFunctionSingleton;
import net.sourceforge.jFuzzyLogic.membership.Value;
import net.sourceforge.jFuzzyLogic.plot.JFuzzyChart;
import net.sourceforge.jFuzzyLogic.rule.LinguisticTerm;
import net.sourceforge.jFuzzyLogic.rule.Variable;
import org.antlr.runtime.RecognitionException;

/**
 * Fuzzy Inference System FuzzyEvaluator
 *
 * @author Diogo Regateiro
 */
public class FuzzyEvaluator {

    final static String FB_VARIABLE_INFERENCE_PHASE_NAME = "VariableInference";
    final static String FB_ACCESS_CONTROL_PHASE_NAME = "AccessControl";

    private static void printToFile(String filename, List<DecisionResult> ret) {
        try {
            try (PrintWriter pw = new PrintWriter(filename + ".txt")) {
                ret.stream().forEachOrdered((result) -> pw.println(result));
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(FuzzyEvaluator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private final FIS fis;

    public FuzzyEvaluator(String fcl, boolean inlineFcl) throws RecognitionException {
        if (inlineFcl) {
            this.fis = FIS.createFromString(fcl, false);
        } else {
            this.fis = FIS.load(fcl, false);
        }
    }

    public Collection<String> getVariableNameList() {
        Set<String> ret = new HashSet<>();
        this.fis.getFunctionBlock(FB_VARIABLE_INFERENCE_PHASE_NAME).variables().stream()
                .filter((variable) -> variable.isInput())
                .forEach((variable) -> ret.add(variable.getName()));
        return ret;
    }

    public Map<String, Variable> evaluate(Map<String, Double> inVariables, boolean debug) {
        Map<String, Variable> ret = new HashMap<>();
        List<Variable> outVariables = new ArrayList<>();

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

            //save the VariableInference output variable to configure the respective AccessControl input.
            outVariables.add(outVariable);

            //add the input value for the AccessControl function block
            fis.setVariable(acfb.getName(), outVariable.getName(), outVariable.getLatestDefuzzifiedValue());
        });

        //for each input variable on the AccessControl functionblock
        acfb.getVariables().values().stream().filter((variable) -> (variable.isInput())).forEach((inVariable) -> {
            //find the respective VariableInference output variable
            outVariables.stream().filter((outVariable) -> (outVariable.getName().equals(inVariable.getName()))).forEach((outVariable) -> {
                //For each linguistic term in the VariableInference output variable
                outVariable.getLinguisticTerms().values().stream().forEach((lt) -> {
                    //Add the linguistic term with x as the defuzzified value and y as the membership degree
                    //This will put all linguistic terms on the same x, which will be the value for the AccessControl input variable
                    DefuzzifierCenterOfGravitySingletons defuzzifier = (DefuzzifierCenterOfGravitySingletons) outVariable.getDefuzzifier();
                    MembershipFunctionSingleton mfunction = new MembershipFunctionSingleton(
                            new Value(outVariable.getValue()),
                            new Value(defuzzifier.getDiscreteValue(lt.getMembershipFunction().getParameter(0)))
                    );
                    inVariable.add(new LinguisticTerm(lt.getTermName(), mfunction));
                });
            });
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

        String testFile = "academic_3vars.fcl";

        switch (testFile) {
            case "academic.fcl": // works
                vars.put("Number_Of_Publications", 12.0);
                vars.put("Number_Of_Citations", 50.0);
        }

        FuzzyEvaluator feval = new FuzzyEvaluator(testFile, false);
//        System.out.println(feval.evaluate(vars, false));
        AbstractFuzzyAnalyser ofanal = new OptimizedFuzzyAnalyser(feval);
        AbstractFuzzyAnalyser sfanal = new SimpleFuzzyAnalyser(feval);

        AbstractFuzzyAnalyser.DecisionResultsToReturn drtr = AbstractFuzzyAnalyser.DecisionResultsToReturn.ONLY_GRANT;

        List<DecisionResult> ret = new ArrayList<>();
        int itr = 1;
        long time = System.nanoTime();
        for (int i = 0; i < itr; i++) {
            ret = ofanal.analyse("Read", drtr);
        }
        time = ((System.nanoTime() - time) / (1000000 * itr));
        System.out.println("OFA took " + time + "ms to process the Read permission, which needed " + ofanal.getNumberOfEvaluations() + " evaluations and found " + ret.size() + " permission results.");

        printToFile("O_Read", ret);

        time = System.nanoTime();
        for (int i = 0; i < itr; i++) {
            ret = sfanal.analyse("Read", drtr);
        }
        time = ((System.nanoTime() - time) / (1000000 * itr));
        System.out.println("SFA took " + time + "ms to process the Read permission, which needed " + sfanal.getNumberOfEvaluations() + " evaluations and found " + ret.size() + " permission results.");

        printToFile("S_Read", ret);

        time = System.nanoTime();
        for (int i = 0; i < itr; i++) {
            ret = ofanal.analyse("Write", drtr);
        }
        time = ((System.nanoTime() - time) / (1000000 * itr));
        System.out.println("OFA took " + time + "ms to process the Write permission, which needed " + ofanal.getNumberOfEvaluations() + " evaluations and found " + ret.size() + " permission results.");

        printToFile("O_Write", ret);

        time = System.nanoTime();
        for (int i = 0; i < itr; i++) {
            ret = sfanal.analyse("Write", drtr);
        }
        time = ((System.nanoTime() - time) / (1000000 * itr));
        System.out.println("SFA took " + time + "ms to process the Write permission, which needed " + sfanal.getNumberOfEvaluations() + " evaluations and found " + ret.size() + " permission results.");

        printToFile("S_Write", ret);
    }

    FIS getFis() {
        return fis;
    }
}
