/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.dfcl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
 *
 * @author Diogo Regateiro <diogoregateiro@ua.pt>
 */
public class DFIS {

    private final FIS fis;
    private final boolean verbose;
    private final Set<DynamicFunction> dfunctions;
    private final List<String> fbNameOrder;

    /**
     * functionblock, varname of the previous block
     */
    private final Map<String, String> connectingVars;

    public DFIS(File dfclFile, boolean verbose) throws IOException, RecognitionException {
        this.dfunctions = new HashSet<>();
        this.connectingVars = new HashMap<>();
        this.fbNameOrder = new ArrayList<>();
        this.verbose = verbose;

        StringBuilder dfcl = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new FileReader(dfclFile))) {
            String line;
            while ((line = in.readLine()) != null) {
                dfcl.append(line).append(System.getProperty("line.separator"));
            }
        }

        this.fis = parse(dfcl.toString());
    }

    public DFIS(String dfclStr, boolean verbose) throws RecognitionException {
        this.dfunctions = new HashSet<>();
        this.connectingVars = new HashMap<>();
        this.fbNameOrder = new ArrayList<>();
        this.verbose = verbose;
        this.fis = parse(dfclStr);
    }

    private FIS parse(String dfclStr) throws RecognitionException {
        StringBuilder fcl = new StringBuilder();

        String functionBlock = "";
        try (BufferedReader in = new BufferedReader(new StringReader(dfclStr))) {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.contains("FUNCTION_BLOCK")) {
                    fcl.append(line).append("\n");
                    Matcher matcher = Pattern.compile("\\s*FUNCTION_BLOCK\\s+(\\w+)\\s*").matcher(line);
                    if (matcher.matches()) {
                        functionBlock = matcher.group(1);
                        fbNameOrder.add(functionBlock);
                    }
                } else if (line.contains("DYNAMIC_FUZZIFY")) {
                    Matcher matcher = Pattern.compile("\\s*DYNAMIC_FUZZIFY\\s+(\\w+)\\s*").matcher(line);
                    if (matcher.matches()) {
                        fcl.append(line.replace("DYNAMIC_", "")).append("\n");

                        while (!(line = in.readLine()).contains("END_DYNAMIC_FUZZIFY")) {
                            if (line.contains("TERM")) {
                                line = line.replace(";", " := (0, 1) ;");
                                fcl.append(line).append("\n");
                            }
                        }
                        fcl.append(line.replace("DYNAMIC_", "")).append("\n");
                    }
                } else if (line.contains("DYNAMIC_RULER")) {
                } else if (line.contains("INDIRECT_CONNECTOR")) {
                    Pattern varPattern = Pattern.compile("\\s*(\\w+)\\s*;\\s*");
                    while (!(line = in.readLine()).contains("END_INDIRECT_CONNECTOR")) {
                        Matcher matcher = varPattern.matcher(line);
                        if (matcher.matches()) {
                            connectingVars.put(functionBlock, matcher.group(1));
                        }
                    }
                } else if (line.contains("DIRECT_CONNECTOR")) {
                    Pattern varPattern = Pattern.compile("\\s*(\\w+)\\s*;\\s*");
                    while (!(line = in.readLine()).contains("END_DIRECT_CONNECTOR")) {
                        Matcher matcher = varPattern.matcher(line);
                        if (matcher.matches()) {
                            connectingVars.put(functionBlock, matcher.group(1));

                            fcl.append("FUZZIFY ").append(matcher.group(1)).append("\n")
                                    .append("END_FUZZIFY").append("\n\n");
                        }
                    }
                } else {
                    fcl.append(line).append("\n");
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(DFIS.class.getName()).log(Level.SEVERE, null, ex);
        }

        return FIS.createFromString(fcl.toString(), this.verbose);
    }

    public void registerDynamicFunction(DynamicFunction dynFunction) {
        this.dfunctions.add(dynFunction);
    }

    public void removeDynamicFunctions() {
        this.dfunctions.clear();
    }

    /**
     * Returns the input variables of the first function block.
     *
     * @return
     */
    public Collection<String> getInputVariableNameList() {
        Set<String> ret = new HashSet<>();
        this.fis.getFunctionBlock(this.fbNameOrder.get(0)).variables().stream()
                .filter((variable) -> variable.isInput())
                .forEach((variable) -> ret.add(variable.getName()));
        return ret;
    }

    public Collection<String> getInputVariableNameList(FunctionBlock functionBlock) {
        Set<String> ret = new HashSet<>();
        functionBlock.variables().stream()
                .filter((variable) -> variable.isInput())
                .forEach((variable) -> ret.add(variable.getName()));
        return ret;
    }

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

        List<FunctionBlock> fbOrder = new ArrayList<>();
        fbNameOrder.forEach((fbName) -> fbOrder.add(fis.getFunctionBlock(fbName)));
        fbOrder.forEach((fb) -> fb.reset());

        for (int i = 0; i < fbOrder.size(); i++) {
            FunctionBlock currFB = fbOrder.get(i);

            // Call the custom functions
            this.dfunctions.forEach((dfunction) -> {
                inVariables.putAll(dfunction.process(currFB));
            });

            // Set inputs as needed
            inVariables.keySet().stream().filter(((varName) -> getInputVariableNameList(currFB).contains(varName))).forEach((varName) -> {
                fis.setVariable(currFB.getName(), varName, inVariables.get(varName));
            });

            // Show 
            if (debug) {
                JFuzzyChart.get().chart(currFB);
            }

            // Evaluate
            currFB.evaluate();

            // If there is another function block after this one
            if (i < fbOrder.size() - 1) {
                List<Variable> outVariables = new ArrayList<>();
                FunctionBlock nextFB = fbOrder.get(i + 1);

                // Save output variables as input for the next functionblock
                currFB.variables().stream().filter((variable) -> (variable.isOutput())).forEach((outVariable) -> {
                    if (debug) {
                        JFuzzyChart.get().chart(outVariable, outVariable.getDefuzzifier(), true);
                    }

                    //save the VariableInference output variable to configure the respective AccessControl input.
                    outVariables.add(outVariable);

                    //add the crisp value as the input for the next function block
                    fis.setVariable(nextFB.getName(), outVariable.getName(), outVariable.getValue());
                });

                //for each input variable on the next functionblock that does not have any LT
                nextFB.getVariables().values().stream().filter((variable) -> variable.isInput() && variable.getLinguisticTerms().isEmpty()).forEach((inVariable) -> {
                    //find the respective previous output variables
                    outVariables.stream().filter((outVariable) -> outVariable.getName().equals(inVariable.getName())).forEach((outVariable) -> {
                        //For each linguistic term in the previous output variables
                        outVariable.getLinguisticTerms().values().stream().forEach((lt) -> {
                            //Add the linguistic term with x as the common crisp value and y as the membership degree
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
            } else {
                // If the current function block is the last one, add the output variables to the result
                currFB.variables().stream().filter((variable) -> (variable.isOutput())).forEach((variable) -> {
                    if (debug) {
                        JFuzzyChart.get().chart(variable, variable.getDefuzzifier(), true);
                    }

                    ret.put(variable.getName(), variable);
                });
            }
        }

        return ret;
    }

    public FIS getFIS() {
        return this.fis;
    }
}
