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
import net.sourceforge.jFuzzyLogic.defuzzifier.Defuzzifier;
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

    private final static Pattern FUNCTION_BLOCK_PATTERN = Pattern.compile("\\s*FUNCTION_BLOCK\\s+(\\w+)\\s*");
    private final static Pattern SOURCE_BLOCK_PATTERN = Pattern.compile("\\s*SOURCE\\s+(\\w+)\\s*");
    private final static Pattern SOURCE_INPUT_PATTERN = Pattern.compile("\\s*INPUT\\s+(\\w+)\\s*;\\s*");
    private final static Pattern ENSEMBLE_FUZZIFY_BLOCK_PATTERN = Pattern.compile("\\s*ENSEMBLE_FUZZIFY\\s+(\\w+)\\s*");
    private final static Pattern ENSEMBLE_FUZZIFY_SOURCE_PATTERN = Pattern.compile("\\s*SOURCE\\s+(\\w+)\\s*;\\s*");
    private final static Pattern ENSEMBLE_FUZZIFY_FB_PATTERN = Pattern.compile("\\s*FUNCTION_BLOCK\\s+(\\w+)\\s*;\\s*");
    private final static Pattern EXTERNAL_FUZZIFY_BLOCK_PATTERN = Pattern.compile("\\s*EXTERNAL_FUZZIFY\\s+(\\w+)\\s*");
    private final static Pattern EXTERNAL_RULEBLOCK_PATTERN = Pattern.compile("\\s*EXTERNAL_RULEBLOCK\\s*(\\w+)\\s*");
    private final static Pattern CRISP_INPUT_CONNECTOR_PATTERN = Pattern.compile("\\s*CRISP_INPUT_CONNECTOR\\s*(\\w+)\\s*");
    private final static Pattern FUZZY_INPUT_CONNECTOR_PATTERN = Pattern.compile("\\s*FUZZY_INPUT_CONNECTOR\\s*(\\w+)\\s*");
    private final static Pattern DEPENDENCY_PATTERN = Pattern.compile("\\s*(\\w+)\\s*;\\s*");
    
    private final FIS fis;
    private final boolean verbose;
    private final Set<EIFS> eifs;
    private final Map<String, DefuzzifyEIOMS> d_ioms;
    private final Map<String, NoDefuzzifyEIOMS> nd_ioms;
    private final Map<String, ESource> esources;
    private final List<SourceIO> sources;
    private final List<EnsembleBlock> ensembles;
    private final List<String> fbNameOrder;

    /**
     * functionblock, dependent block and it's varname
     */
    private final Map<String, List<Entry<String, String>>> connectorDependencies;

    public DFIS(File dfclFile, boolean verbose) throws IOException, RecognitionException {
        this.eifs = new HashSet<>();
        this.d_ioms = new HashMap<>();
        this.nd_ioms = new HashMap<>();
        this.esources = new HashMap<>();
        this.sources = new ArrayList<>();
        this.ensembles = new ArrayList<>();
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
        this.esources = new HashMap<>();
        this.sources = new ArrayList<>();
        this.ensembles = new ArrayList<>();
        this.connectorDependencies = new HashMap<>();
        this.fbNameOrder = new ArrayList<>();
        this.verbose = verbose;
        this.fis = parse(dfclStr);
    }

    private FIS parse(String dfclStr) throws RecognitionException {
        //long time = System.nanoTime();
        StringBuilder fcl = new StringBuilder();

        String functionBlock = "";
        try (BufferedReader in = new BufferedReader(new StringReader(dfclStr))) {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.contains("FUNCTION_BLOCK")) {
                    fcl.append(line).append("\n");
                    Matcher matcher = FUNCTION_BLOCK_PATTERN.matcher(line);
                    if (matcher.matches()) {
                        functionBlock = matcher.group(1);
                        fbNameOrder.add(functionBlock);
                    }
                } else if (line.contains("SOURCE")) {
                    Matcher matcher = SOURCE_BLOCK_PATTERN.matcher(line);

                    if (matcher.matches()) {
                        SourceIO source = new SourceIO(matcher.group(1));
                        while (!(line = in.readLine()).contains("END_SOURCE")) {
                            Matcher inputMatcher = SOURCE_INPUT_PATTERN.matcher(line);

                            if (inputMatcher.matches()) {
                                source.addInput(inputMatcher.group(1));
                            }
                        }
                        this.sources.add(source);
                    }
                } else if (line.contains("ENSEMBLE_FUZZIFY")) {
                    Matcher matcher = ENSEMBLE_FUZZIFY_BLOCK_PATTERN.matcher(line);
                    if (matcher.matches()) {
                        fcl.append(line.replace("ENSEMBLE_", "")).append("\n");

                        EnsembleBlock ensembleBlock = new EnsembleBlock(functionBlock);
                        while (!(line = in.readLine()).contains("END_ENSEMBLE_FUZZIFY")) {
                            Matcher sourceMatcher = ENSEMBLE_FUZZIFY_SOURCE_PATTERN.matcher(line);
                            Matcher fbMatcher = ENSEMBLE_FUZZIFY_FB_PATTERN.matcher(line);

                            if (sourceMatcher.matches()) {
                                ensembleBlock.addSource(sourceMatcher.group(1));
                            } else if (fbMatcher.matches()) {
                                ensembleBlock.setEFIS(fbMatcher.group(1));
                            }
                        }
                        fcl.append(line.replace("ENSEMBLE_", "")).append("\n");
                        this.ensembles.add(ensembleBlock);
                    }
                } else if (line.contains("EXTERNAL_FUZZIFY")) {
                    Matcher matcher = EXTERNAL_FUZZIFY_BLOCK_PATTERN.matcher(line);
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
                    Matcher matcher = EXTERNAL_RULEBLOCK_PATTERN.matcher(line);
                    String outputVar = null;
                    if (matcher.matches()) {
                        outputVar = matcher.group(1);
                        //Create a faux ruleblock
                        fcl.append("RULEBLOCK ").append(outputVar).append("\n")
                                .append("\tAND:MIN;\n").append("\tACCU:MAX;\n")
                                .append("END_RULEBLOCK\n\n");
                    }

                    // Throw an error if such a variable was not declared
                    if (outputVar == null) {
                        throw new RuntimeException("No output var defined for external ruleblock.");
                    }

                    // Find the ruleblock mode of operation
                    // varPattern = Pattern.compile("\\s*MODE\\s*[:]\\s*(\\w+)\\s*;\\s*");
                    while (!(line = in.readLine()).contains("END_EXTERNAL_RULEBLOCK"));
                } else if (line.contains("CRISP_INPUT_CONNECTOR")) {
                    Matcher matcher = CRISP_INPUT_CONNECTOR_PATTERN.matcher(line);
                    String connectedFB = null;
                    // Find the function block dependency
                    if (matcher.matches()) {
                        connectedFB = matcher.group(1);

                        if (!fbNameOrder.contains(connectedFB)) {
                            throw new RuntimeException("Function block " + connectedFB + " was connected but it wasn't previously declared.");
                        }
                    }

                    // Save the dependencies
                    while (!(line = in.readLine()).contains("END_CRISP_INPUT_CONNECTOR")) {
                        matcher = DEPENDENCY_PATTERN.matcher(line);
                        if (matcher.matches()) {
                            connectorDependencies.putIfAbsent(functionBlock, new ArrayList<>());
                            connectorDependencies.get(functionBlock).add(
                                    new AbstractMap.SimpleImmutableEntry<>(connectedFB, matcher.group(1))
                            );
                        }
                    }
                } else if (line.contains("FUZZY_INPUT_CONNECTOR")) {
                    Matcher matcher = FUZZY_INPUT_CONNECTOR_PATTERN.matcher(line);
                    String connectedFB = null;
                    // Find the function block dependency
                    if (matcher.matches()) {
                        connectedFB = matcher.group(1);

                        if (!fbNameOrder.contains(connectedFB)) {
                            throw new RuntimeException("Function block " + connectedFB + " was connected but it wasn't previously declared.");
                        }
                    }

                    // Save the dependencies
                    while (!(line = in.readLine()).contains("END_FUZZY_INPUT_CONNECTOR")) {
                        matcher = DEPENDENCY_PATTERN.matcher(line);
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

        //time = System.nanoTime() - time;
        //System.out.print(time / 1000000.0);
        //System.out.print(" ");
        return FIS.createFromString(fcl.toString(), this.verbose);
    }

    public void registerExternalSource(ESource source) {
        this.esources.put(source.getSourceLabel(), source);
    }

    public void removeAllExternalSources() {
        this.esources.clear();
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

            //long time = System.nanoTime();
            // If the current function block is used as an E.FIS, skip it
            if (ensembles.parallelStream().anyMatch(e -> currFB.getName().equals(e.getEFISName()))) {
                continue;
            }

            // execute the sources
            List<String> efisList = new ArrayList<>();
            ensembles.parallelStream().filter(e -> e.getAFISName().equals(currFB.getName())).forEach(e -> {
                e.getSources().parallelStream().forEach(sourceName -> {
                    inVariables.putAll(esources.get(sourceName).process(inVariables));
                });
                efisList.add(e.getEFISName());
            });

            //execute the E.FIS
            fbOrder.parallelStream().filter(fb -> efisList.contains(fb.getName())).forEach(efis -> {
                // Set inputs as needed
                inVariables.keySet().stream().filter(((varName) -> getInputVariableNameList(efis).contains(varName))).forEach((varName) -> {
                    fis.setVariable(efis.getName(), varName, inVariables.get(varName));
                });

                // evaluate E.FIS
                efis.evaluate();

                // Show 
                if (debug) {
                    JFuzzyChart.get().chart(efis);
                }

                // add fuzzy outputs as fuzzy inputs for A.FIS 
                efis.getVariables().values().parallelStream().filter(var -> var.isOutput()).forEach(outVariable -> {
                    Variable inVariable = currFB.getVariable(outVariable.getName());
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

                    // Set the mapping input
                    inVariable.setValue(outVariable.getValue());
                });
            });
            //time = System.nanoTime() - time;
            //System.out.println(time / 1000000.0);
            
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

            // Evaluate static rule blocks
            currFB.evaluate();

            // Show 
            if (debug) {
                JFuzzyChart.get().chart(currFB);
            }

            // Process the output variables
            currFB.variables().stream().filter((variable) -> (variable.isOutput())).forEach((outVariable) -> {
                // Execute the no_defuzzify external rule blocks
                if (nd_ioms.containsKey(outVariable.getName())) {
                    Map<String, Double> ltValues = nd_ioms.get(outVariable.getName()).process(currFB.variables());
                    DefuzzifierCenterOfGravitySingletons defuzzifier = new DefuzzifierCenterOfGravitySingletons(outVariable);

                    // for each returned linguistic term value
                    ltValues.keySet().stream().forEach((ltName) -> {
                        double ltValue = ltValues.get(ltName);
                        // create a linguistic term for the variable x value and the returned y value
                        LinguisticTerm lt = new LinguisticTerm(ltName, new MembershipFunctionSingleton(
                                new Value(outVariable.getMembershipFunction(ltName).getParameter(0)), new Value(ltValue)
                        ));
                        // add the linguistic term
                        outVariable.add(lt);
                        
                        // set the same point on the defuzzifier
                        defuzzifier.setPoint(outVariable.getMembershipFunction(ltName).getParameter(0), ltValue);
                    });

                    // defuzzify
                    outVariable.setDefuzzifier(defuzzifier);
                    double value = outVariable.defuzzify();
                    outVariable.setValue(value);
                    outVariable.setLatestDefuzzifiedValue(value);
                }

                // execute defuzzify external rule blocks
                if (d_ioms.containsKey(outVariable.getName())) {
                    double crispValue = d_ioms.get(outVariable.getName()).process(currFB.variables());
                    outVariable.setValue(crispValue);
                    outVariable.setLatestDefuzzifiedValue(crispValue);
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
