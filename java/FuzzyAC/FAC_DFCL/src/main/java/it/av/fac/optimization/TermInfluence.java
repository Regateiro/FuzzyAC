/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.optimization;

import it.av.fac.dfcl.DFIS;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import net.sourceforge.jFuzzyLogic.FIS;
import net.sourceforge.jFuzzyLogic.FunctionBlock;
import net.sourceforge.jFuzzyLogic.rule.Rule;
import net.sourceforge.jFuzzyLogic.rule.RuleExpression;
import net.sourceforge.jFuzzyLogic.rule.RuleTerm;
import org.antlr.runtime.RecognitionException;
import org.json.JSONObject;

/**
 *
 * @author Diogo Regateiro
 */
public class TermInfluence {

    public static String CalculateTermsInfluence(FIS fis, String inputFBName, String outputFBName) {
        Map<String, Map<String, Map<String, Double>>> influences = new HashMap<>();

        fis.getFunctionBlock(outputFBName).getVariables().values().stream()
                .filter((var) -> var.isOutput()).map((var) -> var.getName()).forEach((outputVariable) -> {
                    // output variable to rule terms and their actual influence value
                    Map<String, Map<String, Double>> ovarInfluences = new HashMap<>();
                    List<Rule> rules = new ArrayList<>();

                    // for each function block
                    fis.iterator().forEachRemaining((functBlock) -> {
                        functBlock.iterator().forEachRemaining((ruleBlock) -> {
                            // save each rule
                            rules.addAll(ruleBlock.getRules());
                        });
                    });

                    // get the influences from the rules
                    ovarInfluences.putAll(getInfluences(rules, outputVariable));

                    // filter out the terms that do not belong to input variables
                    FunctionBlock inputFB = fis.getFunctionBlock(inputFBName);
                    ovarInfluences.entrySet().removeIf((entry) -> {
                        String varName = entry.getKey().split("[:]")[0];
                        return !inputFB.varibleExists(varName) || inputFB.getVariable(varName).isOutput();
                    });
                    ovarInfluences.values().forEach((termInfluence) -> termInfluence.entrySet().removeIf((entry) -> entry.getValue() == 0));

                    influences.put(outputVariable, ovarInfluences);
                });

        JSONObject ret = new JSONObject(influences);

        fis.getFunctionBlock(outputFBName).getVariables().values().stream()
                .filter((var) -> var.isOutput()).forEach((outputVar) -> {
                    ret.getJSONObject(outputVar.getName()).put("*", outputVar.getLatestDefuzzifiedValue() > 0.5 ? "GRANT" : "DENY");
                });

        // return the influences
        return ret.toString(2);
    }

    private static Map<String, Map<String, Double>> getInfluences(List<Rule> rules, String outputVariable) {
        Map<String, Map<String, Double>> influences = new HashMap<>();

        // pass 1: process grant/deny LTs
        // for each rule
        rules.stream().filter((rule) -> rule.getDegreeOfSupport() > 0).forEach((Rule rule) -> {
            // get the consequence
            RuleTerm outputRuleTerm = rule.getConsequents().get(0);
            Double outputInfluenceMod;

            // process only the influences that match the requested output
            if (outputRuleTerm.getVariable().getName().equalsIgnoreCase(outputVariable)) {

                // if it is grant, set the influence modifier to 1, or -1 if the consequence is negated
                if (outputRuleTerm.getTermName().equalsIgnoreCase("grant")) {
                    outputInfluenceMod = (outputRuleTerm.isNegated() ? -1.0 : 1.0);
                } // if it is deny, set the influence modifier to -1, or 1 if the consequence is negated
                else if (outputRuleTerm.getTermName().equalsIgnoreCase("deny")) {
                    outputInfluenceMod = (outputRuleTerm.isNegated() ? 1.0 : -1.0);
                } // if it isn't either grant or deny, skip it
                else {
                    return;
                }

                // for every rule term in the antecedents
                getRuleTerms(rule.getAntecedents()).stream().forEach((ruleTerm) -> {
                    String varName = ruleTerm.getVariable().getName();
                    String termName = ruleTerm.getTermName();

                    // calculate the influence from the term membership
                    Double currInfluence = outputInfluenceMod * ruleTerm.getMembership();
                    currInfluence *= (ruleTerm.isNegated() ? -1 : 1);

                    // if this rule term has appeared in a previous rule
                    if (influences.containsKey(varName) && influences.get(varName).containsKey(termName)) {
                        // then the influence sign must match, if not set it to 0 as it is impossible to determine how it changes the output
                        Double prevInfluence = influences.get(varName).get(termName);
                        if (prevInfluence * currInfluence < 0.0) {
                            influences.get(varName).put(termName, 0.0);
                        }
                    } // if it hasn't appeared on a rule before, save the current influence 
                    else {
                        influences.putIfAbsent(varName, new HashMap<>());
                        influences.get(varName).put(termName, currInfluence);
                    }
                });
            }
        });

        // pass 2+ (do it while the list was modified)
        AtomicBoolean wasUpdated = new AtomicBoolean();
        do {
            wasUpdated.set(false);

            // for each rule with some support
            rules.stream().filter((rule) -> rule.getDegreeOfSupport() > 0).forEach((Rule rule) -> {
                // get the consequence
                RuleTerm outputRuleTerm = rule.getConsequents().get(0);
                String outputVarName = outputRuleTerm.getVariable().getName();
                String outputTermName = outputRuleTerm.getTermName();

                // if the consequence has an associated influence value
                if (influences.containsKey(outputVarName) && influences.get(outputVarName).containsKey(outputTermName)) {
                    Double influence = influences.get(outputVarName).get(outputTermName);

                    // then for each of the antecendent terms
                    getRuleTerms(rule.getAntecedents()).stream().forEach((ruleTerm) -> {
                        String varName = ruleTerm.getVariable().getName();
                        String termName = ruleTerm.getTermName();

                        // calculate the term influence from its membership degree
                        Double currInfluence = ruleTerm.getMembership();
                        currInfluence *= (ruleTerm.isNegated() ? -1 : 1);
                        currInfluence *= (influence < 0 ? -1 : 1);

                        // if this rule term has appeared in a previous rule
                        if (influences.containsKey(varName) && influences.get(varName).containsKey(termName)) {
                            // then the influence sign must match, if not set it to 0 as it is impossible to determine how it changes the output
                            Double prevInfluence = influences.get(varName).get(termName);
                            if (prevInfluence * currInfluence < 0.0) {
                                influences.get(varName).put(termName, 0.0);
                                wasUpdated.set(true);
                            }
                        } // if it hasn't appeared on a rule before, save the current influence 
                        else {
                            influences.putIfAbsent(varName, new HashMap<>());
                            influences.get(varName).put(termName, currInfluence);
                            wasUpdated.set(true);
                        }
                    });
                }
            });
        } while (wasUpdated.get());

        return influences;
    }

    private static List<RuleTerm> getRuleTerms(RuleExpression antecedents) {
        List<RuleTerm> ruleTerms = new ArrayList<>();

        Object o1 = antecedents.getTerm1();
        Object o2 = antecedents.getTerm2();

        if (antecedents.isFuzzyRuleExpression(o1)) {
            ruleTerms.addAll(getRuleTerms((RuleExpression) o1));
        } else if (antecedents.isFuzzyRuleTerm(o1)) {
            ruleTerms.add((RuleTerm) o1);
        }

        if (antecedents.isFuzzyRuleExpression(o2)) {
            ruleTerms.addAll(getRuleTerms((RuleExpression) o2));
        } else if (antecedents.isFuzzyRuleTerm(o2)) {
            ruleTerms.add((RuleTerm) o2);
        }

        return ruleTerms;
    }

    public static void main(String[] args) throws RecognitionException, IOException {
        Map<String, Double> vars = new HashMap<>();

        String testFile = "academic.fcl";

        switch (testFile) {
            case "academic.dfcl": // works
            case "academic.fcl": // works
                vars.put("Number_Of_Publications", 12.0);
                vars.put("Number_Of_Citations", 50.0);
        }

        testFile = "..\\FAC_Module_Decision\\" + testFile;

        DFIS dfis = new DFIS(new File(testFile), new String[]{"VariableInference", "AccessControl"}, false);
        System.out.println(dfis.evaluate(vars, false));
        System.out.println(CalculateTermsInfluence(dfis.getFIS(), "VariableInference", "AccessControl"));
    }
}
