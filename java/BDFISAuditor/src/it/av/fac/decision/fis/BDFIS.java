/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.decision.fis;

import it.av.fac.decision.util.decision.AlphaCutDecisionMaker;
import it.av.fac.decision.util.handlers.ValidatorHandler;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
 * Fuzzy Inference System with support for FB chaining and dynamic functions
 *
 * @author Diogo Regateiro <diogoregateiro@ua.pt>
 */
public class BDFIS {

    public final static String FB_VARIABLE_INFERENCE_PHASE_NAME = "VariableInference";
    public final static String FB_ACCESS_CONTROL_PHASE_NAME = "AccessControl";

    private final FIS fis;

    public BDFIS(File dfclFile) throws IOException, RecognitionException {
        StringBuilder dfcl = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new FileReader(dfclFile))) {
            String line;
            while ((line = in.readLine()) != null) {
                dfcl.append(line).append(System.getProperty("line.separator"));
            }
        }

        this.fis = parse(dfcl.toString());
    }

    public BDFIS(String dfclStr) throws RecognitionException {
        this.fis = parse(dfclStr);
    }

    private FIS parse(String dfclStr) throws RecognitionException {
        return FIS.createFromString(dfclStr, true);
    }

    /**
     * Returns the input variables of the first function block.
     *
     * @param fbName Name of the function block.
     * @return
     */
    public Collection<String> getInputVariableNameList(String fbName) {
        Set<String> ret = new HashSet<>();
        this.fis.getFunctionBlock(fbName).variables().stream()
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

    public Collection<String> getOutputVariableNameList(FunctionBlock functionBlock) {
        Set<String> ret = new HashSet<>();
        functionBlock.variables().stream()
                .filter((variable) -> variable.isOutput())
                .forEach((variable) -> ret.add(variable.getName()));
        return ret;
    }

    private List<FunctionBlock> getFBOrder(Set<String> inVarNames) {
        List<FunctionBlock> fbOrder = new ArrayList<>();
        Set<String> availableVars = new HashSet<>(inVarNames);

        // fb skipped flag, used to determine when an fb is not added to the list so the list has to be reiterated.
        boolean fbSkipped;
        do {
            // set the fb skipped flag to false.
            fbSkipped = false;
            int fbCount = fbOrder.size();

            // iterate over the list of function blocks
            Iterator<FunctionBlock> itr = fis.iterator();
            while (itr.hasNext()) {
                FunctionBlock fb = itr.next();

                // if the order list already contains the function block, continue to the next
                if (!fbOrder.contains(fb)) {
                    // otherwise, check if the input variables available are enough to run the function block
                    if (availableVars.containsAll(getInputVariableNameList(fb))) {
                        // if so, then it can be executed at this point, add it to the order list and its outputs to the list of available input variables
                        availableVars.addAll(getOutputVariableNameList(fb));
                        fbOrder.add(fb);
                    } else {
                        // if the available input variables do not cover all the fb inputs, skip it.
                        fbSkipped = true;
                    }
                }
            }

            // if a function block was skipped and the fbOrder was not added to, then there is input variable missing.
            if (fbSkipped && fbCount == fbOrder.size()) {
                System.err.println("It was not possible to build a function block dependency order, one or more input variables cannot be resolved.");
                System.err.println("Check if all input variables were either provided an input or calculated by a fis, and that there are no cyclic dependencies.");
                System.err.println("List of function blocks with missing inputs:");
                itr = fis.iterator();
                while (itr.hasNext()) {
                    FunctionBlock fb = itr.next();
                    if (!fbOrder.contains(fb)) {
                        System.err.println(" -> " + fb.getName());
                    }
                }
                System.exit(2);
            }
        } while (fbSkipped);

        return fbOrder;
    }

    public void printFB(FunctionBlock fb) {
        System.out.println(" - BLOCK: " + fb.getName());
        fb.variables().stream()
                .filter((variable) -> variable.isInput())
                .forEach((variable) -> {
                    System.out.println(" |");
                    System.out.println(String.format(" |-- INPUT: %s (%f)", variable.getName(), variable.getValue()));
                    variable.getLinguisticTerms().values().forEach((term) -> {
                        System.out.println(String.format(" | |-- TERM: %s (%f)", term.getTermName(), variable.getMembership(term.getTermName())));
                    });
                });

        fb.variables().stream()
                .filter((variable) -> variable.isOutput())
                .forEach((variable) -> {
                    System.out.println(" |");
                    System.out.println(String.format(" |-- OUTPUT: %s (%f)", variable.getName(), variable.getLatestDefuzzifiedValue()));
                    variable.getLinguisticTerms().values().forEach((term) -> {
                        if (variable.getDefuzzifier().isDiscrete()) {
                            DefuzzifierCenterOfGravitySingletons defuzzifier = (DefuzzifierCenterOfGravitySingletons) variable.getDefuzzifier();
                            System.out.println(String.format(" | |-- TERM: %s (%f)", term.getTermName(), defuzzifier.getDiscreteValue(term.getMembershipFunction().getParameter(0))));
                        } else {
                            System.out.println(String.format(" | |-- TERM: %s (%f)", term.getTermName(), variable.getMembership(term.getTermName())));
                        }
                    });
                });

        System.out.println();
    }

    public Map<String, Variable> evaluate(Map<String, Double> inVariables, boolean verbose, boolean chart) {
        Map<String, Variable> ret = new HashMap<>();

        // Error while loading?
        if (fis == null) {
            System.err.println("Not initialized.");
            return null;
        }

        if (verbose) {
            System.out.println("********* PARSE *********");
            System.out.println(fis);
            System.out.println("\n********* INPUT *********");
            System.out.println(inVariables);
            System.out.println("\n******* INFERENCE *******");
        }

        List<FunctionBlock> fbOrder = getFBOrder(inVariables.keySet());

        for (int i = 0; i < fbOrder.size(); i++) {
            FunctionBlock currFB = fbOrder.get(i);

            // Set inputs as needed
            inVariables.keySet().stream().filter(((varName) -> getInputVariableNameList(currFB).contains(varName))).forEach((varName) -> {
                fis.setVariable(currFB.getName(), varName, inVariables.get(varName));
            });

            // Show 
            if (chart) {
                JFuzzyChart.get().chart(currFB);
            }

            // Evaluate
            currFB.evaluate();

            if (verbose) {
                printFB(currFB);
            }

            // If there is another function block after this one
            if (i < fbOrder.size() - 1) {
                List<Variable> outVariables = new ArrayList<>();
                FunctionBlock nextFB = fbOrder.get(i + 1);

                // Save output variables as input for the next functionblock
                currFB.variables().stream().filter((variable) -> (variable.isOutput())).forEach((outVariable) -> {
                    if (chart) {
                        JFuzzyChart.get().chart(outVariable, outVariable.getDefuzzifier(), true);
                    }

                    //save the VariableInference output variable to configure the respective AccessControl input.
                    outVariables.add(outVariable);

                    //add the crisp value as the input for the next function block
                    fis.setVariable(nextFB.getName(), outVariable.getName(), outVariable.getValue());
                });

                //for each input variable on the next functionblock that does not have any LT
                nextFB.getVariables().values().stream().filter((variable) -> variable.isInput()).forEach((inVariable) -> {
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
            } else {
                // If the current function block is the last one, add the output variables to the result
                currFB.variables().stream().filter((variable) -> (variable.isOutput())).forEach((variable) -> {
                    if (chart) {
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

    public static void main(String[] args) throws Exception {
        String[] permissions = null;
        String fcl = null;
        boolean save = false;
        boolean validate = false;
        boolean permutate = false;

        for (String arg : args) {
            String[] fields = arg.split("[=]");
            if (fields[0].toLowerCase().endsWith(".fcl")) {
                fcl = fields[0];
            } else if (fields[0].equalsIgnoreCase("permutate")) {
                permutate = true;
            } else if (fields[0].equalsIgnoreCase("validate")) {
                validate = true;
            } else if (fields[0].equalsIgnoreCase("saveDecisions")) {
                save = true;
            } else if (fields[0].equalsIgnoreCase("permissions")) {
                permissions = fields[1].split("[,]");
            }
        }

        if (fcl == null || permissions == null) {
            System.err.println("Usage: BDFISAuditor <path/to/file.fcl> permissions=p1[,p2,...] [permutate] [validate] [saveDecisions]");
            System.exit(1);
        }

        BDFIS bdfis = new BDFIS(new File(fcl));
        String fclName = fcl.split("[.]")[0];

        if (permutate) {
            for (List<String> varOrder : getPermutations(bdfis.getInputVariableNameList(FB_VARIABLE_INFERENCE_PHASE_NAME))) {
                validate(bdfis, varOrder, permissions, validate, save, fclName);
            }
        } else {
            validate(bdfis, null, permissions, validate, save, fclName);
        }
    }

    public static void validate(BDFIS bdfis, List<String> varOrder, String[] permissions, boolean validate, boolean save, String fclName) throws FileNotFoundException {
        AbstractFuzzyAnalyser ofanal = new OBDFISAuditor(bdfis);
        AbstractFuzzyAnalyser sfanal = new SBDFISAuditor(bdfis);

        for (String permission : permissions) {
            try (ValidatorHandler handler = new ValidatorHandler()) {
                if (save) {
                    handler.setOutputFile(String.format("%s_%s.txt", fclName, permission));
                }
                handler.enableHandler();

                if (varOrder != null) {
                    ofanal.setVariableOrdering(varOrder);
                }
                ofanal.analyse(permission, new AlphaCutDecisionMaker(0.5), handler, false);

                handler.enableValidation();

                if (validate) {
                    sfanal.setVariableOrdering(ofanal.getVariableOrdering());
                    sfanal.analyse(permission, new AlphaCutDecisionMaker(0.5), handler, false);
                }

                System.out.println("Permission: " + permission);
                System.out.println(" --> Permutation: " + ofanal.getVariableOrdering());
                if (validate) {
                    System.out.println(" --> Validation:  " + (handler.wasValidationSuccessul() ? "OK!" : "KO!"));
                }
                System.out.println(" --> #Evl-Calls:  " + ofanal.getNumberOfEvaluations());
                if (validate) {
                    System.out.println(" --> #Max-Calls:  " + sfanal.getNumberOfEvaluations());
                }
                System.out.println();
            }
        }
    }

    /**
     * Creates a list of all permutations of the given list.
     *
     * @param inputVariableNameList The list of items to create permutations of.
     * @return
     */
    public static List<List<String>> getPermutations(Collection<String> inputVariableNameList) {
        List<List<String>> ret = new ArrayList<>();
        getPermutationsRec(new ArrayList<>(inputVariableNameList), new ArrayList<>(), ret);
        return ret;
    }

    /**
     * Creates a list of all permutations of the given list.
     *
     * @param variableNameList The list of name of variables.
     * @param tempList A temporary list to be used internally.
     * @param acc The list of permutations where each permutation is to be added
     * to.
     */
    private static void getPermutationsRec(List<String> variableNameList, List<String> tempList, List<List<String>> acc) {
        if (variableNameList.size() != tempList.size()) {
            variableNameList.stream().filter((varName) -> (!tempList.contains(varName)))
                    .forEachOrdered((varName) -> {
                        tempList.add(varName);
                        getPermutationsRec(variableNameList, tempList, acc);
                        tempList.remove(varName);
                    });
        } else {
            acc.add(new ArrayList<>(tempList));
        }
    }
}
