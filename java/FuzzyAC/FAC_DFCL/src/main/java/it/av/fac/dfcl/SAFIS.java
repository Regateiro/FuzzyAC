/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.dfcl;

import it.av.fac.dfcl.factory.FISFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
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
 * @author Diogo Regateiro
 */
public class SAFIS {

    private final FIS safis;
    private final FIS fis;

    public SAFIS(File fisFCL, boolean verbose) throws IOException, RecognitionException {
        StringBuilder fcl = new StringBuilder();
        StringBuilder safcl = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new FileReader(fisFCL))) {
            String line;
            boolean parsingSAFIS = false;
            boolean skipBlock = false;
            final FISFactory factory = new FISFactory();
            int fisNum = 1;

            while ((line = in.readLine()) != null) {
                if (line.startsWith("SOURCE_AGGREGATOR")) {
                    parsingSAFIS = true;
                    factory.reset();

                    line = line.trim();
                    String[] fields = Arrays.asList(line.split("[ ;:=\t]")).parallelStream().filter((field) -> field.length() > 0).toArray(String[]::new);
                    factory.addOutputVar(fields[1]);
                } else if (line.startsWith("END_SOURCE_AGGREGATOR")) {
                    // generate rules
                    factory.getOutputVars().forEach((outputVar) -> {

                        List<String> terms = factory.getInputVarTerms().values().stream().findAny().get();
                        terms.forEach((term) -> {
                            List<String> ruleParts = new ArrayList<>();

                            factory.getInputVars().forEach((inputVar) -> {
                                ruleParts.add(inputVar);
                                ruleParts.add(term.split("[ ]")[1]);
                            });
                            ruleParts.add(outputVar);
                            ruleParts.add(term.split("[ ]")[1]);

                            factory.addRule(outputVar, "OR", ruleParts.toArray(new String[0]));
                        });

                    });

                    safcl.append(factory.toFCL("safis" + fisNum++));
                    parsingSAFIS = false;
                } else if (parsingSAFIS) {
                    line = line.trim();
                    String[] fields = Arrays.asList(line.split("[ ;:=(),\t]")).parallelStream().filter((field) -> field.length() > 0).toArray(String[]::new);

                    if (fields.length == 0) {
                        continue;
                    }

                    switch (fields[0]) {
                        case "INPUT":
                            factory.addInputVar(fields[1], Double.valueOf(fields[2]));
                            break;
                        case "TERM":
                            for (String var : factory.getVars()) {
                                double[] xy = new double[fields.length - 2];
                                for (int i = 2; i < fields.length; i++) {
                                    xy[i - 2] = Double.valueOf(fields[i]);
                                }
                                factory.addVarTerm(var, fields[1], xy);
                            }
                            break;
                        case "DEFUZZIFY_METHOD":
                            factory.getOutputVars().stream().forEach((outputVar) -> {
                                factory.addDeffuzifyMethod(outputVar, fields[1]);
                            });
                            break;
                        case "RULEBLOCK_ACT":
                            factory.getOutputVars().stream().forEach((outputVar) -> {
                                factory.addRuleACTMethod(outputVar, fields[1]);
                            });
                            break;
                        case "RULEBLOCK_AND":
                            factory.getOutputVars().stream().forEach((outputVar) -> {
                                factory.addRuleANDMethod(outputVar, fields[1]);
                            });
                            break;
                        case "RULEBLOCK_ACCU":
                            factory.getOutputVars().stream().forEach((outputVar) -> {
                                factory.addRuleACCUMethod(outputVar, fields[1]);
                            });
                    }
                } else if (line.contains("AGGREGATOR_FUZZIFY") && !skipBlock) {
                    fcl.append(line.replace("AGGREGATOR_", "")).append(System.lineSeparator());
                    fcl.append("\tEND_FUZZIFY").append(System.lineSeparator());
                    skipBlock = true;
                } else if (line.contains("END_") && skipBlock) {
                    skipBlock = false;
                } else if (!skipBlock) {
                    fcl.append(line).append(System.getProperty("line.separator"));
                }
            }

            safis = FIS.createFromString(safcl.toString(), verbose);
            fis = FIS.createFromString(fcl.toString(), verbose);
        }
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
        ReentrantLock retLock = new ReentrantLock();

        // Error while loading?
        if (safis == null) {
            System.err.println("Not initialized.");
            return null;
        }

        // Print ruleSet
        if (debug) {
            System.out.println(safis);
        }

        List<FunctionBlock> safisList = new ArrayList<>();
        Iterator<FunctionBlock> itr = safis.iterator();
        while (itr.hasNext()) {
            safisList.add(itr.next());
        }

        safisList.parallelStream().forEach((currFIS) -> {
            // Set inputs as needed
            inVariables.keySet().stream().filter(((varName) -> getInputVariableNameList(currFIS).contains(varName))).forEach((varName) -> {
                safis.setVariable(currFIS.getName(), varName, inVariables.get(varName));
            });

            // Show 
            JFuzzyChart.get().chart(currFIS.variables().stream().findAny().get(), debug);

            // Evaluate
            currFIS.evaluate();

            // add the output to the result
            currFIS.variables().stream().filter((variable) -> (variable.isOutput())).forEach((variable) -> {
                JFuzzyChart.get().chart(variable, variable.getDefuzzifier(), debug);

                retLock.lock();
                try {
                    ret.put(variable.getName(), variable);
                } finally {
                    retLock.unlock();
                }
            });
        });

        // Execute the FIS using the aggregated outputs
        FunctionBlock fisFB = fis.iterator().next();

        List<Variable> outVariables = new ArrayList<>();

        // Save output variables as input for the next functionblock
        ret.values().stream().filter((variable) -> (variable.isOutput())).forEach((outVariable) -> {
            JFuzzyChart.get().chart(outVariable, outVariable.getDefuzzifier(), debug);

            //save the VariableInference output variable to configure the respective AccessControl input.
            outVariables.add(outVariable);

            //add the crisp value as the input for the next function block
            fis.setVariable(fisFB.getName(), outVariable.getName(), outVariable.getValue());
        });

        //for each input variable on the next functionblock that does not have any LT
        fisFB.getVariables().values().stream().filter((variable) -> variable.isInput() && variable.getLinguisticTerms().isEmpty()).forEach((inVariable) -> {
            //find the respective previous output variables
            outVariables.stream().filter((outVariable) -> outVariable.getName().equals(inVariable.getName())).forEach((outVariable) -> {
                //For each linguistic term in the previous output variables
                outVariable.getLinguisticTerms().values().stream().forEach((lt) -> {
                    //Add the linguistic term with x as the common crisp value and y as the membership degree
                    //This will put all linguistic terms on the same x, which will be the value for the AccessControl input variable
                    MembershipFunctionSingleton mfunction;
                    if (outVariable.getDefuzzifier().isDiscrete()) {
                        DefuzzifierCenterOfGravitySingletons defuzzifier = (DefuzzifierCenterOfGravitySingletons) outVariable.getDefuzzifier();
                        mfunction = new MembershipFunctionSingleton(
                                new Value(outVariable.getValue()),
                                new Value(defuzzifier.getDiscreteValue(lt.getMembershipFunction().getParameter(0)))
                        );
                    } else {
                        mfunction = new MembershipFunctionSingleton(
                                new Value(outVariable.getValue()),
                                new Value(outVariable.getMembership(lt.getTermName()))
                        );
                    }
                    inVariable.add(new LinguisticTerm(lt.getTermName(), mfunction));
                });
            });
        });

        // Show 
        if (debug) {
            JFuzzyChart.get().chart(fisFB);
        }

        // Evaluate
        fisFB.evaluate();

        // add the output to the result
        fisFB.variables().stream().filter((variable) -> (variable.isOutput())).forEach((variable) -> {
            JFuzzyChart.get().chart(variable, variable.getDefuzzifier(), debug);

            retLock.lock();
            try {
                ret.put(variable.getName(), variable);
            } finally {
                retLock.unlock();
            }
        });

        return ret;
    }

    public FIS getFIS() {
        return this.fis;
    }

    public FIS getSAFIS() {
        return this.safis;
    }

    public static void main(String[] args) throws Exception {
        Map<String, Double> vars = new HashMap<>();

        String fisPath = args[0];
        String inputPath = args[1];
        
        try(BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(new File(inputPath)), Charset.forName("UTF-8")))) {
            String line;
            while((line = in.readLine()) != null) {
                String[] fields = line.split("[ =]");
                vars.put(fields[0], Double.parseDouble(fields[1]));
            }
        } catch (NumberFormatException | IndexOutOfBoundsException ex) {
            System.err.println("Error: Malformed input file.");
            System.exit(1);
        }

        SAFIS safis = new SAFIS(new File(fisPath), true);
        System.out.println(safis.evaluate(vars, true));
    }
}
