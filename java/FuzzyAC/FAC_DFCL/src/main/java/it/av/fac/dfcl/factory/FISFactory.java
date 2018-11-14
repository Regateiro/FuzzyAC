/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.dfcl.factory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.sourceforge.jFuzzyLogic.FIS;
import org.antlr.runtime.RecognitionException;

/**
 *
 * @author Diogo Regateiro
 */
public class FISFactory {

    private final Map<String, Double> var_weights;
    private final Map<String, List<String>> fuzzify_blocks;
    private final Map<String, List<String>> defuzzify_blocks;
    private final Map<String, List<String>> rule_blocks;

    public FISFactory() {
        this.var_weights = new HashMap<>();
        this.fuzzify_blocks = new HashMap<>();
        this.defuzzify_blocks = new HashMap<>();
        this.rule_blocks = new HashMap<>();
    }

    public void addInputVar(String varName, double ymax) {
        fuzzify_blocks.putIfAbsent(varName, new ArrayList<>());
        var_weights.put(varName, ymax);
    }

    public void addOutputVar(String varName) {
        defuzzify_blocks.putIfAbsent(varName, new ArrayList<>());
    }

    public void addVarTerm(String varName, String termName, double... xy) {
        // The number of points must be either 1 or even
        if (xy.length == 0 || (xy.length > 1 && xy.length % 2 != 0)) {
            throw new IllegalArgumentException("The number of xy points must either be 1 (singleton) or even.");
        }

        StringBuilder termStr = new StringBuilder("TERM ");
        termStr.append(termName).append(" := ");

        if (xy.length == 1) {
            termStr.append(xy[0]);
        } else {
            for (int i = 0; i < xy.length; i += 2) {
                termStr.append("(").append(xy[i]).append(",")
                        .append(Math.min(var_weights.get(varName), xy[i + 1])).append(")");
            }
        }
        termStr.append(";");

        if (fuzzify_blocks.keySet().contains(varName)) {
            fuzzify_blocks.get(varName).add(termStr.toString());
        } else if (defuzzify_blocks.keySet().contains(varName)) {
            defuzzify_blocks.get(varName).add(termStr.toString());
        } else {
            throw new IllegalArgumentException(String.format("Undefined variable name [%s].", varName));
        }
    }

    public Set<String> getInputVars() {
        return fuzzify_blocks.keySet();
    }
    
    public Map<String, List<String>> getInputVarTerms() {
        return fuzzify_blocks;
    }

    public Set<String> getOutputVars() {
        return defuzzify_blocks.keySet();
    }

    public void addDeffuzifyMethod(String varName, String method) {
        defuzzify_blocks.putIfAbsent(varName, new ArrayList<>());
        defuzzify_blocks.get(varName).add(String.format("METHOD : %s ;", method));
    }

    public void addRule(String ruleBlockName, String connector, String... ruleParts) {
        if (ruleParts.length < 4 || ruleParts.length % 2 != 0) {
            throw new IllegalArgumentException("The number of rule parts must be at least 4 and even.");
        }

        rule_blocks.putIfAbsent(ruleBlockName, new ArrayList<>());

        StringBuilder rule = new StringBuilder();
        rule.append("RULE ").append(rule_blocks.get(ruleBlockName).parallelStream().filter((r) -> r.startsWith("RULE")).count()).append(": ");
        String prefix = "IF";
        for (int i = 0; i < ruleParts.length - 2; i += 2) {
            rule.append(prefix).append(" (");
            rule.append(ruleParts[i]).append(" IS ").append(ruleParts[i + 1]).append(") ");
            prefix = connector;
        }
        rule.append("THEN ").append(ruleParts[ruleParts.length - 2])
                .append(" IS ").append(ruleParts[ruleParts.length - 1])
                .append(" ;");

        rule_blocks.get(ruleBlockName).add(rule.toString());
    }

    public void addRuleACTMethod(String ruleBlockName, String act) {
        rule_blocks.putIfAbsent(ruleBlockName, new ArrayList<>());
        rule_blocks.get(ruleBlockName).add("ACT:" + act + ";");
    }

    public void addRuleANDMethod(String ruleBlockName, String and) {
        rule_blocks.putIfAbsent(ruleBlockName, new ArrayList<>());
        rule_blocks.get(ruleBlockName).add("AND:" + and + ";");
    }

    public void addRuleACCUMethod(String ruleBlockName, String accu) {
        rule_blocks.putIfAbsent(ruleBlockName, new ArrayList<>());
        rule_blocks.get(ruleBlockName).add("ACCU:" + accu + ";");
    }

    public String toFCL(String fisName) {
        StringBuilder fcl = new StringBuilder();

        // Declare the root node
        fcl.append("FUNCTION_BLOCK ").append(fisName).append(System.lineSeparator());

        // Add the input variable declarations
        fcl.append("\tVAR_INPUT").append(System.lineSeparator());
        fuzzify_blocks.keySet().stream().forEach((inputVar) -> {
            fcl.append("\t\t").append(inputVar).append(" : REAL;").append(System.lineSeparator());
        });
        fcl.append("\tEND_VAR").append(System.lineSeparator());

        fcl.append(System.lineSeparator());

        // Add the output variable declarations
        fcl.append("\tVAR_OUTPUT").append(System.lineSeparator());
        defuzzify_blocks.keySet().stream().forEach((inputVar) -> {
            fcl.append("\t\t").append(inputVar).append(" : REAL;").append(System.lineSeparator());
        });
        fcl.append("\tEND_VAR").append(System.lineSeparator());

        fcl.append(System.lineSeparator());

        // Add the fuzzify blocks
        fuzzify_blocks.keySet().stream().forEach((inputVar) -> {
            fcl.append("\tFUZZIFY ").append(inputVar).append(System.lineSeparator());
            fuzzify_blocks.get(inputVar).stream().forEach((term) -> {
                fcl.append("\t\t").append(term).append(System.lineSeparator());
            });
            fcl.append("\tEND_FUZZIFY").append(System.lineSeparator());
        });

        fcl.append(System.lineSeparator());

        // Add the defuzzify blocks
        defuzzify_blocks.keySet().stream().forEach((outputVar) -> {
            fcl.append("\tDEFUZZIFY ").append(outputVar).append(System.lineSeparator());
            defuzzify_blocks.get(outputVar).stream().forEach((term) -> {
                fcl.append("\t\t").append(term).append(System.lineSeparator());
            });
            fcl.append("\tEND_DEFUZZIFY").append(System.lineSeparator());
        });

        fcl.append(System.lineSeparator());

        // Add the rule blocks
        rule_blocks.keySet().forEach((ruleBlock) -> {
            fcl.append("\tRULEBLOCK ").append(ruleBlock).append(System.lineSeparator());
            rule_blocks.get(ruleBlock).forEach((rule) -> {
                fcl.append("\t\t").append(rule).append(System.lineSeparator());
            });
            fcl.append("\tEND_RULEBLOCK").append(System.lineSeparator());
        });

        // close the root node and return
        fcl.append("END_FUNCTION_BLOCK").append(System.lineSeparator());
        return fcl.toString();
    }

    public FIS build(String name) throws RecognitionException {
        return FIS.createFromString(toFCL(name), true);
    }

    public void reset() {
        defuzzify_blocks.clear();
        fuzzify_blocks.clear();
        rule_blocks.clear();
        var_weights.clear();
    }
}
