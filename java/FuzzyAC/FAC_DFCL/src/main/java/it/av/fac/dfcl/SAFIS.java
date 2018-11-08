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
import java.util.ArrayList;
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
import net.sourceforge.jFuzzyLogic.plot.JFuzzyChart;
import net.sourceforge.jFuzzyLogic.rule.Variable;
import org.antlr.runtime.RecognitionException;

/**
 *
 * @author Diogo Regateiro
 */
public class SAFIS {

    private final FIS safis;
    private final FIS fis;
    private final boolean verbose;

    public SAFIS(File safisFCL, File fisFCL, boolean verbose) throws IOException, RecognitionException {
        this.verbose = verbose;

        StringBuilder fcl = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new FileReader(safisFCL))) {
            String line;
            while ((line = in.readLine()) != null) {
                fcl.append(line).append(System.getProperty("line.separator"));
            }
        }

        this.safis = parse(fcl.toString());
        
        fcl = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new FileReader(fisFCL))) {
            String line;
            while ((line = in.readLine()) != null) {
                fcl.append(line).append(System.getProperty("line.separator"));
            }
        }

        this.fis = parse(fcl.toString());
    }

    private FIS parse(String dfclStr) throws RecognitionException {
        return FIS.createFromString(dfclStr, this.verbose);
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
            if (debug) {
                JFuzzyChart.get().chart(currFIS);
            }

            // Evaluate
            currFIS.evaluate();

            // add the output to the result
            currFIS.variables().stream().filter((variable) -> (variable.isOutput())).forEach((variable) -> {
                if (debug) {
                    JFuzzyChart.get().chart(variable, variable.getDefuzzifier(), true);
                }

                retLock.lock();
                try {
                    ret.put(variable.getName(), variable);
                } finally {
                    retLock.unlock();
                }
            });
        });
        
        // Execute the FIS using the aggregated outputs

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

        String safisFile = "safis_tipping.fcl";
        String fis = "tipping.fcl";

        switch (safisFile) {
            case "safis_tipping.fcl": // works
                vars.put("service_op_payer", Math.floor(Math.random() * 10));
                vars.put("service_op_A", Math.floor(Math.random() * 10));
                vars.put("service_op_B", Math.floor(Math.random() * 10));
                vars.put("service_op_C", Math.floor(Math.random() * 10));
                vars.put("service_op_D", Math.floor(Math.random() * 10));
                vars.put("service_op_E", Math.floor(Math.random() * 10));
                vars.put("service_op_F", Math.floor(Math.random() * 10));
                vars.put("service_op_G", Math.floor(Math.random() * 10));
                vars.put("service_op_H", Math.floor(Math.random() * 10));
                vars.put("service_op_I", Math.floor(Math.random() * 10));
                vars.put("food_op_payer", Math.floor(Math.random() * 10));
                vars.put("food_op_A", Math.floor(Math.random() * 10));
                vars.put("food_op_B", Math.floor(Math.random() * 10));
                vars.put("food_op_C", Math.floor(Math.random() * 10));
                vars.put("food_op_D", Math.floor(Math.random() * 10));
                vars.put("food_op_E", Math.floor(Math.random() * 10));
                vars.put("food_op_F", Math.floor(Math.random() * 10));
                vars.put("food_op_G", Math.floor(Math.random() * 10));
                vars.put("food_op_H", Math.floor(Math.random() * 10));
                vars.put("food_op_I", Math.floor(Math.random() * 10));
        }

        SAFIS safis = new SAFIS(new File(safisFile), new File(fis), true);
        System.out.println(safis.evaluate(vars, false));
    }
}
