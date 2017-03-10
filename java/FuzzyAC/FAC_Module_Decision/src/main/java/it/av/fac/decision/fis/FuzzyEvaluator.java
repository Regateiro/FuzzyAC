/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.decision.fis;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import net.sourceforge.jFuzzyLogic.FIS;
import net.sourceforge.jFuzzyLogic.FunctionBlock;
import net.sourceforge.jFuzzyLogic.plot.JFuzzyChart;
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

    private static void evaluate(FIS fis, Map<String, Double> variables) {
        // Error while loading?
        if (fis == null) {
            System.err.println("Can't read FCL string.");
            return;
        }

        FunctionBlock functionBlock = fis.getFunctionBlock(null);

        // Show 
        JFuzzyChart.get().chart(functionBlock);

        // Set inputs
        variables.keySet().forEach((varName) -> {
            fis.setVariable(varName, variables.get(varName));
        });

        // Evaluate
        fis.evaluate();

        // Show output variable's chart
        Variable expertise = functionBlock.getVariable("Expertise");
        JFuzzyChart.get().chart(expertise, expertise.getDefuzzifier(), true);

        // Print ruleSet
        System.out.println(fis);
    }

    public static void main(String[] args) throws Exception {
        Map<String, Double> vars = new HashMap<>();
        
        vars.put("Number_Of_Publications", 100.0);
        vars.put("Time_Since_Last_Publication", 2.0);
        vars.put("Number_Of_Citations", 1000.0);
        
        FuzzyEvaluator.evaluateFromFile("test.fcl", vars);
    }
}
