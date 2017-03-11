/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.decision.fis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    /**
     * Loads the fcl file and evaluates the input into an output.
     *
     * @param fclFileName The FCL file path
     * @param variables The variable inputs.
     * @throws RecognitionException
     * @throws java.io.IOException
     */
    public static void evaluateFromFile(String fclFileName, Map<String, Double> variables) throws RecognitionException, IOException {
        evaluate(FIS.load(fclFileName, false), variables);
    }

    /**
     * Loads the fcl file and evaluates the input into an output.
     *
     * @param fclString The FCL in string format
     * @param variables The variable inputs.
     * @throws RecognitionException
     */
    public static void evaluateFrmString(String fclString, Map<String, Double> variables) throws RecognitionException {
        evaluate(FIS.createFromString(fclString, false), variables);
    }

    private static void evaluate(FIS fis, Map<String, Double> inVariables) {
        List<Variable> outVariables = new ArrayList<>();

        // Error while loading?
        if (fis == null) {
            System.err.println("Can't read FCL string.");
            return;
        }

        // Print ruleSet
        System.out.println(fis);

        FunctionBlock vifb = fis.getFunctionBlock("VariableInference");
        FunctionBlock acfb = fis.getFunctionBlock("AccessControl");

        // Set inputs
        inVariables.keySet().forEach((varName) -> {
            fis.setVariable(vifb.getName(), varName, inVariables.get(varName));
        });

        // Show 
        JFuzzyChart.get().chart(vifb);
        
        // Evaluate
        vifb.evaluate();

        // Save output variables as input for the next functionblock
        vifb.variables().stream().filter((variable) -> (variable.isOutput())).forEach((outVariable) -> {
            JFuzzyChart.get().chart(outVariable, outVariable.getDefuzzifier(), true);
            
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
                    DefuzzifierCenterOfGravitySingletons defuzzifier = (DefuzzifierCenterOfGravitySingletons)outVariable.getDefuzzifier();
                    MembershipFunctionSingleton mfunction = new MembershipFunctionSingleton(
                            new Value(outVariable.getValue()),
                            new Value(defuzzifier.getDiscreteValue(lt.getMembershipFunction().getParameter(0)))
                    );
                    inVariable.add(new LinguisticTerm(lt.getTermName(), mfunction));
                });
            });
        });

        // Show 
        JFuzzyChart.get().chart(acfb);
        
        // Evaluate
        acfb.evaluate();

        acfb.variables().stream().filter((variable) -> (variable.isOutput())).forEach((variable) -> {
            JFuzzyChart.get().chart(variable, variable.getDefuzzifier(), true);
        });
    }

    public static void main(String[] args) throws Exception {
        Map<String, Double> vars = new HashMap<>();

        String testFile = "test3.fcl";

        switch (testFile) {
            case "test.fcl":
                vars.put("Number_Of_Publications", 10.0);
                vars.put("Time_Since_Last_Publication", 0.0);
                vars.put("Number_Of_Citations", 8.0);
                break;
            case "test2.fcl":
            case "test3.fcl": // works
                vars.put("Number_Of_Publications", 12.0);
                vars.put("Number_Of_Citations", 50.0);
        }

        FuzzyEvaluator.evaluateFromFile(testFile, vars);
    }
}
