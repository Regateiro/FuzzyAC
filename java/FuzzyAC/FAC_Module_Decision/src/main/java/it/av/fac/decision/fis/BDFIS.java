/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.decision.fis;

import it.av.fac.decision.util.decision.AlphaCutDecisionMaker;
import it.av.fac.decision.util.handlers.ValidatorHandler;
import it.av.fac.dfcl.DFIS;
import it.av.fac.dfcl.DynamicFunction;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import net.sourceforge.jFuzzyLogic.FIS;
import net.sourceforge.jFuzzyLogic.rule.Variable;
import org.antlr.runtime.RecognitionException;

/**
 * Fuzzy Inference System BDFIS
 *
 * @author Diogo Regateiro <diogoregateiro@ua.pt>
 */
public class BDFIS {
    public final static String FB_VARIABLE_INFERENCE_PHASE_NAME = "VariableInference";
    public final static String FB_ACCESS_CONTROL_PHASE_NAME = "AccessControl";

    protected final DFIS dfis;

    public BDFIS(String fcl, boolean inlineFcl) throws RecognitionException, IOException {
        if (inlineFcl) {
            this.dfis = new DFIS(fcl, false);
        } else {
            this.dfis = new DFIS(new File(fcl), false);
        }
    }
    
    public void registerDynamicFunction(DynamicFunction dynFunction) {
        this.dfis.registerDynamicFunction(dynFunction);
    }

    public void removeDynamicFunctions() {
        this.dfis.removeDynamicFunctions();
    }

    public Map<String, Variable> evaluate(Map<String, Double> inVariables, boolean debug) {
        return this.dfis.evaluate(inVariables, debug);
    }

    public FIS getFIS() {
        return this.dfis.getFIS();
    }
    
    public Collection<String> getInputVariableNameList() {
        return this.dfis.getInputVariableNameList();
    }

    public static void main(String[] args) throws Exception {
        Map<String, Double> vars = new HashMap<>();

        String testFile = "academic.dfcl";

        switch (testFile) {
            case "academic.dfcl": // works
                vars.put("Number_Of_Publications", 12.0);
                vars.put("Number_Of_Citations", 50.0);
        }

        BDFIS bdfis = new BDFIS(testFile, false);
        System.out.println(bdfis.evaluate(vars, true));

        AbstractFuzzyAnalyser ofanal = new OBDFISAuditor(bdfis);
        AbstractFuzzyAnalyser sfanal = new OBDFISAuditor(bdfis);

        String[] permissions = new String[]{"Write"};

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
}
