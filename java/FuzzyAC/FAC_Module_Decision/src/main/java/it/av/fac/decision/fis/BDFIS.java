/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.decision.fis;

import it.av.fac.decision.util.decision.AlphaCutDecisionMaker;
import it.av.fac.decision.util.handlers.NullHandler;
import it.av.fac.decision.util.handlers.ValidatorHandler;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
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
 * Fuzzy Inference System BDFIS
 *
 * @author Diogo Regateiro
 */
public class BDFIS {

    public final static String FB_VARIABLE_INFERENCE_PHASE_NAME = "VariableInference";
    public final static String FB_ACCESS_CONTROL_PHASE_NAME = "AccessControl";
    protected final FIS fis;

    public BDFIS(String fcl, boolean inlineFcl) throws RecognitionException {
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

            //add the input value 0 for variable in the AccessControl function block
            fis.setVariable(acfb.getName(), outVariable.getName(), 0);
        });

        //for each input variable on the AccessControl functionblock
        acfb.getVariables().values().stream().filter((variable) -> (variable.isInput())).forEach((inVariable) -> {
            //find the respective VariableInference output variable
            outVariables.stream().filter((outVariable) -> (outVariable.getName().equals(inVariable.getName()))).forEach((outVariable) -> {
                //For each linguistic term in the VariableInference output variable
                outVariable.getLinguisticTerms().values().stream().forEach((lt) -> {
                    //Add the linguistic term with x as 0 and y as the membership degree
                    //This will put all linguistic terms on the same x, which will be the value for the AccessControl input variable
                    DefuzzifierCenterOfGravitySingletons defuzzifier = (DefuzzifierCenterOfGravitySingletons) outVariable.getDefuzzifier();
                    MembershipFunctionSingleton mfunction = new MembershipFunctionSingleton(
                            new Value(0),
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

        String testFile = "academic.fcl";

        switch (testFile) {
            case "academic.fcl": // works
                vars.put("Number_Of_Publications", 12.0);
                vars.put("Number_Of_Citations", 50.0);
        }

        BDFIS bdfis = new BDFIS(testFile, false);
//        System.out.println(bdfis.evaluate(vars, true));

        AbstractFuzzyAnalyser ofanal = new OBDFISAuditor(bdfis);
        AbstractFuzzyAnalyser sfanal = new OBDFISAuditor(bdfis);

        String[] permissions = new String[]{"Read", "Write"};

        ValidatorHandler handler = new ValidatorHandler();
        for (String permission : permissions) {
            handler.enableHandler();
            ofanal.analyse(permission, new AlphaCutDecisionMaker(0.5), handler, false);

            sfanal.setVariableOrdering(ofanal.getVariableOrdering());
            handler.enableValidation();

            sfanal.analyse(permission, new AlphaCutDecisionMaker(0.5), handler, false);
            System.out.println("Permission: " + permission + " --> Validation: " + (handler.wasValidationSuccessul() ? "OK!" : "KO!"));
            handler.reset();
        }
    }

    public FIS getFIS() {
        return fis;
    }
}
