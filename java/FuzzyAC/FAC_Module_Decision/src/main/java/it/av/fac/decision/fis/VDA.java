/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.decision.fis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.sourceforge.jFuzzyLogic.FIS;
import net.sourceforge.jFuzzyLogic.FunctionBlock;
import net.sourceforge.jFuzzyLogic.rule.Rule;
import net.sourceforge.jFuzzyLogic.rule.RuleBlock;
import net.sourceforge.jFuzzyLogic.rule.RuleExpression;
import net.sourceforge.jFuzzyLogic.rule.RuleTerm;

/**
 * Variable Dependence Analyser.
 *
 * @author Diogo Regateiro
 */
public class VDA {

    private final FIS fis;

    /**
     * Objects that keep the output rule terms that are used for granting/deny
     * permission.
     */
    private final Map<String, Collection<RuleTerm>> grantingOutputRuleTerms;
    private final Map<String, Collection<RuleTerm>> denyingOutputRuleTerms;

    /**
     * Objects that keep the input rule terms that are used for only
     * granting/deny permission.
     */
    private final Map<String, Collection<RuleTerm>> onlyGrantingInputRuleTerms;
    private final Map<String, Collection<RuleTerm>> onlyDenyingInputRuleTerms;

    public VDA(FIS fis) {
        this.fis = fis;
        this.grantingOutputRuleTerms = new HashMap<>();
        this.denyingOutputRuleTerms = new HashMap<>();
        this.onlyGrantingInputRuleTerms = new HashMap<>();
        this.onlyDenyingInputRuleTerms = new HashMap<>();
    }

    public void analyse() {
        //access the AC rulebook and get the associated rules.
        FunctionBlock fb_ac = this.fis.getFunctionBlock(FuzzyEvaluator.FB_ACCESS_CONTROL_PHASE_NAME);
        HashMap<String, RuleBlock> rb_ac = fb_ac.getRuleBlocks();

        //for each rulebook
        rb_ac.keySet().stream().forEach((String permission) -> {
            RuleBlock rb = rb_ac.get(permission);

            //fore each rule in the rulebook
            rb.getRules().stream().forEach((Rule rule) -> {
                List<RuleTerm> r_rts = new ArrayList<>();

                //obtain all the rule terms from the rule into r_rts
                setRuleTermsFromRule(rule.getAntecedents(), r_rts);

                if (rule.getConsequents().size() > 1) {
                    System.err.println("more than one consequence in a rule. Using only the first one.");
                }

                //check if the rule results into a grant or deny and adds the rule terms to the revelant list.
                RuleTerm consequent = rule.getConsequents().getFirst();
                if (consequent.getLinguisticTerm().getTermName().equalsIgnoreCase("grant")) {
                    grantingOutputRuleTerms.putIfAbsent(permission, new ArrayList<>());
                    grantingOutputRuleTerms.get(permission).addAll(r_rts);
                } else {
                    denyingOutputRuleTerms.putIfAbsent(permission, new ArrayList<>());
                    denyingOutputRuleTerms.get(permission).addAll(r_rts);
                }
            });
        });

        //remove any duplicate ruleterms on each list.
        grantingOutputRuleTerms.values().parallelStream().forEach((ruleTermList) -> removeDuplicateRuleTerms(ruleTermList));
        denyingOutputRuleTerms.values().parallelStream().forEach((ruleTermList) -> removeDuplicateRuleTerms(ruleTermList));

        //access the VI rulebook and get the associated rules.
        FunctionBlock fb_vi = this.fis.getFunctionBlock(FuzzyEvaluator.FB_VARIABLE_INFERENCE_PHASE_NAME);
        HashMap<String, RuleBlock> rb_vi = fb_vi.getRuleBlocks();

        //for each rulebook
        rb_vi.keySet().stream().forEach((String outputvar) -> {
            RuleBlock rb = rb_vi.get(outputvar);
            rb.getRules().stream().forEach((Rule rule) -> {
                List<RuleTerm> r_rts = new ArrayList<>();

                //obtain all the rule terms from the rule into r_rts
                setRuleTermsFromRule(rule.getAntecedents(), r_rts);

                if (rule.getConsequents().size() > 1) {
                    System.err.println("more than one consequence in a rule. Using only the first one.");
                }

                //check if the consequent of the rule is in the granting set or the denying set and add it to the appropriate set.
                RuleTerm consequent = rule.getConsequents().getFirst();
                grantingOutputRuleTerms.keySet().stream().forEach((String permission) -> {
                    if (RuleTermComparator.collectionContains(grantingOutputRuleTerms.get(permission), consequent)) {
                        onlyGrantingInputRuleTerms.putIfAbsent(permission, new ArrayList<>());
                        onlyGrantingInputRuleTerms.get(permission).addAll(r_rts);
                    }
                });

                denyingOutputRuleTerms.keySet().stream().forEach((String permission) -> {
                    if (RuleTermComparator.collectionContains(denyingOutputRuleTerms.get(permission), consequent)) {
                        onlyDenyingInputRuleTerms.putIfAbsent(permission, new ArrayList<>());
                        onlyDenyingInputRuleTerms.get(permission).addAll(r_rts);
                    }
                });
            });
        });

        // remove duplicates
        onlyGrantingInputRuleTerms.values().parallelStream().forEach((ruleTermList) -> removeDuplicateRuleTerms(ruleTermList));
        onlyDenyingInputRuleTerms.values().parallelStream().forEach((ruleTermList) -> removeDuplicateRuleTerms(ruleTermList));

        // remove common terms from both sets.
        onlyGrantingInputRuleTerms.keySet().parallelStream()
                .filter((gperm) -> onlyDenyingInputRuleTerms.keySet().contains(gperm))
                .forEach((gperm) -> removeCommonTerms(onlyGrantingInputRuleTerms.get(gperm), onlyDenyingInputRuleTerms.get(gperm)));
    }

    /**
     * Obtains all the RuleTerms from every RuleExpression and places them in
     * r_rts.
     *
     * @param antecedents
     * @param r_rts
     */
    private void setRuleTermsFromRule(RuleExpression antecedents, Collection<RuleTerm> r_rts) {
        Object t1 = antecedents.getTerm1();
        Object t2 = antecedents.getTerm2();

        //if t1 is an expression
        if (antecedents.isFuzzyRuleExpression(t1)) {
            //recursive call this function to deal with the expression
            setRuleTermsFromRule((RuleExpression) t1, r_rts);
        } else if (antecedents.isFuzzyRuleTerm(t1)) {
            //if it is a ruleterm, add it to the list if it isn't there already.
            if (!RuleTermComparator.collectionContains(r_rts, (RuleTerm) t1)) {
                r_rts.add((RuleTerm) t1);
            }
        }

        //same deal as above but for t2
        if (antecedents.isFuzzyRuleExpression(t2)) {
            setRuleTermsFromRule((RuleExpression) t2, r_rts);
        } else if (antecedents.isFuzzyRuleTerm(t2)) {
            if (!RuleTermComparator.collectionContains(r_rts, (RuleTerm) t2)) {
                r_rts.add((RuleTerm) t2);
            }
        }
    }

    /**
     * Guarantees that the given collection only has distinct ruleterms. 
     * @param ruleTermList 
     */
    private void removeDuplicateRuleTerms(Collection<RuleTerm> ruleTermList) {
        List<RuleTerm> temp = new ArrayList<>();

        ruleTermList.stream()
                .filter((rt) -> !RuleTermComparator.collectionContains(temp, rt))
                .forEach((rt) -> temp.add(rt));

        ruleTermList.clear();
        ruleTermList.addAll(temp);
    }

    /**
     * Removes common terms between two collections of ruleterms.
     * @param ruleTermList1
     * @param ruleTermList2 
     */
    private void removeCommonTerms(Collection<RuleTerm> ruleTermList1, Collection<RuleTerm> ruleTermList2) {
        List<RuleTerm> temp1 = new ArrayList<>();
        List<RuleTerm> temp2 = new ArrayList<>();

        ruleTermList1.stream()
                .filter((rt) -> !RuleTermComparator.collectionContains(ruleTermList2, rt))
                .forEach((rt) -> temp1.add(rt));

        ruleTermList2.stream()
                .filter((rt) -> !RuleTermComparator.collectionContains(ruleTermList1, rt))
                .forEach((rt) -> temp2.add(rt));

        ruleTermList1.clear();
        ruleTermList1.addAll(temp1);
        ruleTermList2.clear();
        ruleTermList2.addAll(temp2);
    }

    /**
     * A class mean to perform comparative operations on ruleterms.
     */
    private static class RuleTermComparator {

        /**
         * Checks if two ruleterms are the same (same variable name and same linguistic term)
         * @param o1
         * @param o2
         * @return 
         */
        public static boolean equals(RuleTerm o1, RuleTerm o2) {
            return o1.getVariable().getName().equals(o2.getVariable().getName())
                    && o1.getTermName().equals(o2.getTermName());
        }

        /**
         * Checks if a ruleterm collection contains a given ruleterm.
         * @param col
         * @param rt
         * @return 
         */
        public static boolean collectionContains(Collection<RuleTerm> col, RuleTerm rt) {
            return col.stream().anyMatch((rtincol) -> (RuleTermComparator.equals(rtincol, rt)));
        }

    }
}
