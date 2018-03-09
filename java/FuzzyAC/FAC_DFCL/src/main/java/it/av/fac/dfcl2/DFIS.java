/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.dfcl2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
    private final Set<EIFS> eifs;
    private final Map<String, DefuzzifyEIOMS> d_ioms;
    private final Map<String, NoDefuzzifyEIOMS> nd_ioms;
    private final List<String> fbNameOrder;

    /**
     * functionblock, dependent block and it's varname
     */
    private final Map<String, List<Entry<String, String>>> connectorDependencies;

    public DFIS(File dfclFile, boolean verbose) throws IOException, RecognitionException {
        this.eifs = new HashSet<>();
        this.d_ioms = new HashMap<>();
        this.nd_ioms = new HashMap<>();
        this.connectorDependencies = new HashMap<>();
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
        this.eifs = new HashSet<>();
        this.d_ioms = new HashMap<>();
        this.nd_ioms = new HashMap<>();
        this.connectorDependencies = new HashMap<>();
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
                } else if (line.contains("EXTERNAL_FUZZIFY")) {
                    Matcher matcher = Pattern.compile("\\s*EXTERNAL_FUZZIFY\\s+(\\w+)\\s*").matcher(line);
                    if (matcher.matches()) {
                        fcl.append(line.replace("EXTERNAL_", "")).append("\n");

                        while (!(line = in.readLine()).contains("END_EXTERNAL_FUZZIFY")) {
                            if (line.contains("TERM")) {
                                line = line.replace(";", " := (0, 1) ;");
                                fcl.append(line).append("\n");
                            }
                        }
                        fcl.append(line.replace("EXTERNAL_", "")).append("\n");
                    }
                } else if (line.contains("EXTERNAL_RULEBLOCK")) {
                    // Determine which output variable the external ruleblock applies to
                    Pattern varPattern = Pattern.compile("\\s*EXTERNAL_RULEBLOCK\\s*(\\w+)\\s*");
                    Matcher matcher = varPattern.matcher(line);
                    String outputVar = null;
                    if (matcher.matches()) {
                        outputVar = matcher.group(1);
                        //Create a faux ruleblock
                        fcl.append("RULEBLOCK ").append(outputVar).append("\n")
                                .append("\tAND:MIN;\n").append("\tACCU_MAX;\n")
                                .append("END_RULEBLOCK\n\n");
                    }

                    // Throw an error if such a variable was not declared
                    if (outputVar == null) {
                        throw new RuntimeException("No output var defined for external ruleblock.");
                    }

                    // Find the ruleblock mode of operation
                    // varPattern = Pattern.compile("\\s*MODE\\s*[:]\\s*(\\w+)\\s*;\\s*");
                    while (!(line = in.readLine()).contains("END_EXTERNAL_RULEBLOCK")) {
//                        matcher = varPattern.matcher(line);
//                        if (matcher.matches()) {
//                            // Check if the necessary external service was registered.
//                            switch (matcher.group(1).toUpperCase()) {
//                                case "DEFUZZIFY":
//                                    if (!d_ioms.keySet().contains(outputVar)) {
//                                        throw new RuntimeException("No external ruleblock service registered for output variable " + outputVar + ", or the incorrect mode is declared.");
//                                    }
//                                    break;
//                                case "NO_DEFUZZIFY":
//                                    if (!nd_ioms.keySet().contains(outputVar)) {
//                                        throw new RuntimeException("No external ruleblock service registered for output variable " + outputVar + ", or the incorrect mode is declared.");
//                                    }
//                                    break;
//                                default:
//                                    throw new RuntimeException("Unrecognized external ruleblock mode: " + matcher.group(1));
//                            }
//                        }
                    }
                } else if (line.contains("CRISP_INPUT_CONNECTOR")) {
                    Pattern varPattern = Pattern.compile("\\s*CRISP_INPUT_CONNECTOR\\s*(\\w+)\\s*");
                    Matcher matcher = varPattern.matcher(line);
                    String connectedFB = null;
                    // Find the function block dependency
                    if (matcher.matches()) {
                        connectedFB = matcher.group(1);

                        if (!fbNameOrder.contains(connectedFB)) {
                            throw new RuntimeException("Function block " + connectedFB + " was connected but it wasn't previously declared.");
                        }
                    }

                    // Save the dependencies
                    varPattern = Pattern.compile("\\s*(\\w+)\\s*;\\s*");
                    while (!(line = in.readLine()).contains("END_CRISP_INPUT_CONNECTOR")) {
                        matcher = varPattern.matcher(line);
                        if (matcher.matches()) {
                            connectorDependencies.putIfAbsent(functionBlock, new ArrayList<>());
                            connectorDependencies.get(functionBlock).add(
                                    new AbstractMap.SimpleImmutableEntry<>(connectedFB, matcher.group(1))
                            );
                        }
                    }
                } else if (line.contains("FUZZY_INPUT_CONNECTOR")) {
                    Pattern varPattern = Pattern.compile("\\s*FUZZY_INPUT_CONNECTOR\\s*(\\w+)\\s*");
                    Matcher matcher = varPattern.matcher(line);
                    String connectedFB = null;
                    // Find the function block dependency
                    if (matcher.matches()) {
                        connectedFB = matcher.group(1);

                        if (!fbNameOrder.contains(connectedFB)) {
                            throw new RuntimeException("Function block " + connectedFB + " was connected but it wasn't previously declared.");
                        }
                    }

                    // Save the dependencies
                    varPattern = Pattern.compile("\\s*(\\w+)\\s*;\\s*");
                    while (!(line = in.readLine()).contains("END_FUZZY_INPUT_CONNECTOR")) {
                        matcher = varPattern.matcher(line);
                        if (matcher.matches()) {
                            connectorDependencies.putIfAbsent(functionBlock, new ArrayList<>());
                            connectorDependencies.get(functionBlock).add(
                                    new AbstractMap.SimpleImmutableEntry<>(connectedFB, matcher.group(1))
                            );

                            // Add the faux fuzzify block
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

    public void registerExternalInputFuzzifierService(EIFS eifs) {
        this.eifs.add(eifs);
    }

    public void removeAllExternalInputFuzzifierServices() {
        this.eifs.clear();
    }

    public void registerExternalIOMappingService(DefuzzifyEIOMS deioms) {
        this.d_ioms.put(deioms.getOutputVariableLabel(), deioms);
    }

    public void registerExternalIOMappingService(NoDefuzzifyEIOMS ndeioms) {
        this.nd_ioms.put(ndeioms.getOutputVariableLabel(), ndeioms);
    }

    public void removeAllExternalIOMappingServices() {
        this.d_ioms.clear();
        this.nd_ioms.clear();
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

            // Set inputs as needed
            inVariables.keySet().stream().filter(((varName) -> getInputVariableNameList(currFB).contains(varName))).forEach((varName) -> {
                fis.setVariable(currFB.getName(), varName, inVariables.get(varName));
            });

            // Call the external input defuzzifiers functions for this function block
            this.eifs.stream()
                    .filter((service) -> currFB.varibleExists(service.getInputVariableLabel()))
                    .filter((service) -> currFB.getVariable(service.getInputVariableLabel()).isInput())
                    .forEach((service) -> {
                        // get the input for this service
                        double input = currFB.getVariable(service.getInputVariableLabel()).getValue();
                        // call the service to obtain the linguistic term membership degrees
                        Map<String, Double> termValues = service.process(input);

                        // for each term
                        termValues.keySet().forEach((term) -> {
                            // get its value
                            double value = termValues.get(term);
                            // change the membership function so that it returns the expected membership degree given the input
                            currFB.getVariable(service.getInputVariableLabel()).getLinguisticTerm(term).setMembershipFunction(
                                    new MembershipFunctionSingleton(
                                            new Value(input),
                                            new Value(value)
                                    )
                            );
                        });
                    });

            // Set connector inputs
            if (connectorDependencies.containsKey(currFB.getName())) {
                for (Entry<String, String> dependency : connectorDependencies.get(currFB.getName())) {
                    FunctionBlock depFB = fis.getFunctionBlock(dependency.getKey());
                    Variable depVariable = depFB.getVariable(dependency.getValue());
                    Variable inVariable = currFB.getVariable(depVariable.getName());

                    if (inVariable.getLinguisticTerms().isEmpty()) {
                        // The fuzzify block is empty, copy the linguistic terms.
                        depVariable.getLinguisticTerms().values().stream().forEach((lt) -> {
                            //Add the linguistic term with x as the common crisp value and y as the membership degree
                            //This will put all linguistic terms on the same x, which will be the value for the AccessControl input variable
                            DefuzzifierCenterOfGravitySingletons defuzzifier = (DefuzzifierCenterOfGravitySingletons) depVariable.getDefuzzifier();
                            MembershipFunctionSingleton mfunction = new MembershipFunctionSingleton(
                                    new Value(depVariable.getValue()),
                                    new Value(defuzzifier.getDiscreteValue(lt.getMembershipFunction().getParameter(0)))
                            );
                            inVariable.add(new LinguisticTerm(lt.getTermName(), mfunction));
                        });
                    }

                    // Set the crisp input
                    fis.setVariable(currFB.getName(), depVariable.getName(), depVariable.getValue());
                }
            }

            // Show 
            if (debug) {
                JFuzzyChart.get().chart(currFB);
            }

            // Evaluate static rule blocks
            currFB.evaluate();

            // Process the output variables
            currFB.variables().stream().filter((variable) -> (variable.isOutput())).forEach((outVariable) -> {
                // Execute the no_defuzzify external rule blocks
                if (nd_ioms.containsKey(outVariable.getName())) {
                    Map<String, Double> ltValues = nd_ioms.get(outVariable.getName()).process(currFB.variables());

                    // for each returned linguistic term value
                    ltValues.keySet().stream().forEach((ltName) -> {
                        double ltValue = ltValues.get(ltName);
                        // create a linguistic term for the variable x value and the returned y value
                        LinguisticTerm lt = new LinguisticTerm(ltName, new MembershipFunctionSingleton(
                                new Value(outVariable.getValue()), new Value(ltValue)
                        ));
                        // add the linguistic term
                        outVariable.add(lt);
                    });

                    // defuzzify
                    outVariable.setValue(outVariable.getDefuzzifier().defuzzify());
                }

                // execute defuzzify external rule blocks
                if (d_ioms.containsKey(outVariable.getName())) {
                    double crispValue = d_ioms.get(outVariable.getName()).process(currFB.variables());
                    outVariable.setValue(crispValue);
                }

                // show graphs
                if (debug) {
                    JFuzzyChart.get().chart(outVariable, outVariable.getDefuzzifier(), true);
                }

                // Add to result
                ret.put(outVariable.getName(), outVariable);
            });
        }

        return ret;
    }

    public FIS getFIS() {
        return this.fis;
    }
}
