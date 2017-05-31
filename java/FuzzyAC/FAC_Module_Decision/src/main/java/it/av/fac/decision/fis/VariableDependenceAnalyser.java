/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.decision.fis;

import it.av.fac.decision.util.variables.MultiRangeValue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
public class VariableDependenceAnalyser {

    private final FIS fis;

    /**
     * Objects that keep the output rule terms that are used for granting/deny
     * permission.
     */
    private final Map<String, List<RuleTerm>> grantingOutputRuleTerms;
    private final Map<String, List<RuleTerm>> denyingOutputRuleTerms;

    /**
     * Objects that keep the input rule terms that are used for only
     * granting/deny permission.
     */
    private final Map<String, List<RuleTerm>> onlyGrantingInputRuleTerms;
    private final Map<String, List<RuleTerm>> onlyDenyingInputRuleTerms;

    //A list of variables that appeared in the rulebooks
    private final Set<String> usedVariables;

    public VariableDependenceAnalyser(FIS fis) {
        this.fis = fis;
        this.grantingOutputRuleTerms = new HashMap<>();
        this.denyingOutputRuleTerms = new HashMap<>();
        this.onlyGrantingInputRuleTerms = new HashMap<>();
        this.onlyDenyingInputRuleTerms = new HashMap<>();
        this.usedVariables = new HashSet<>();
    }

    public void analyse(String permAnalyse) {
        //clear variables
        this.grantingOutputRuleTerms.clear();
        this.denyingOutputRuleTerms.clear();
        this.onlyGrantingInputRuleTerms.clear();
        this.onlyDenyingInputRuleTerms.clear();

        //access the AC rulebook and get the associated rules.
        FunctionBlock fb_ac = this.fis.getFunctionBlock(FuzzyEvaluator.FB_ACCESS_CONTROL_PHASE_NAME);
        HashMap<String, RuleBlock> rb_ac = fb_ac.getRuleBlocks();

        if (!rb_ac.keySet().contains(permAnalyse)) {
            System.err.println("[VariableDependenceAnalyser] : Permission " + permAnalyse + " not defined.");
            System.exit(1);
        }

        //for each rule in the rulebook
        rb_ac.get(permAnalyse).getRules().stream().forEach((Rule rule) -> {
            List<RuleTerm> r_rts = new ArrayList<>();

            //obtain all the rule terms from the rule into r_rts
            setRuleTermsFromRule(rule.getAntecedents(), r_rts);

            if (rule.getConsequents().size() > 1) {
                System.err.println("more than one consequence in a rule. Using only the first one.");
            }

            //check if the rule results into a grant or deny and adds the rule terms to the revelant list.
            RuleTerm consequent = rule.getConsequents().getFirst();
            if (consequent.getLinguisticTerm().getTermName().equalsIgnoreCase("grant")) {
                grantingOutputRuleTerms.putIfAbsent(permAnalyse, new ArrayList<>());
                grantingOutputRuleTerms.get(permAnalyse).addAll(r_rts);
            } else {
                denyingOutputRuleTerms.putIfAbsent(permAnalyse, new ArrayList<>());
                denyingOutputRuleTerms.get(permAnalyse).addAll(r_rts);
            }
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

        //add used variables into the used variables set
        onlyGrantingInputRuleTerms.get(permAnalyse).stream().map((rt) -> rt.getVariable().getName()).forEach((varName) -> usedVariables.add(varName));
        onlyDenyingInputRuleTerms.get(permAnalyse).stream().map((rt) -> rt.getVariable().getName()).forEach((varName) -> usedVariables.add(varName));

        // remove common terms from both sets.
        onlyGrantingInputRuleTerms.keySet().parallelStream()
                .filter((gperm) -> onlyDenyingInputRuleTerms.keySet().contains(gperm))
                .forEach((gperm) -> removeCommonTerms(onlyGrantingInputRuleTerms.get(gperm), onlyDenyingInputRuleTerms.get(gperm)));
    }

    private void setRuleTermsFromRule(RuleExpression antecedents, List<RuleTerm> r_rts) {
        Object t1 = antecedents.getTerm1();
        Object t2 = antecedents.getTerm2();

        if (antecedents.isFuzzyRuleExpression(t1)) {
            setRuleTermsFromRule((RuleExpression) t1, r_rts);
        } else if (antecedents.isFuzzyRuleTerm(t1)) {
            r_rts.add((RuleTerm) t1);
        }

        if (antecedents.isFuzzyRuleExpression(t2)) {
            setRuleTermsFromRule((RuleExpression) t2, r_rts);
        } else if (antecedents.isFuzzyRuleTerm(t2)) {
            r_rts.add((RuleTerm) t2);
        }
    }

    private void removeDuplicateRuleTerms(List<RuleTerm> ruleTermList) {
        List<RuleTerm> temp = new ArrayList<>();

        ruleTermList.stream()
                .filter((rt) -> !RuleTermComparator.collectionContains(temp, rt))
                .forEach((rt) -> temp.add(rt));

        ruleTermList.clear();
        ruleTermList.addAll(temp);
    }

    private void removeCommonTerms(List<RuleTerm> ruleTermList1, List<RuleTerm> ruleTermList2) {
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

    public boolean variableIsUsed(String varName) {
        return usedVariables.contains(varName);
    }

    /**
     * Optimizes the ordering the of the variables.
     *
     * @param variableMap The list of variable in any order.
     */
    public void optimizeOrdering(List<MultiRangeValue> variableMap) {
        //sort the variable according to the amount of NONE contribution
        Collections.sort(variableMap, (o1, o2) -> {
            return Integer.compare(o2.getTotalNotUnknownContribution(), o1.getTotalNotUnknownContribution());
        });
        
//        //put the variable with the least amount of NONE contribution on the end.
//        variableMap.add(variableMap.remove(0));
        
        System.out.println(variableMap);
    }

    private static class RuleTermComparator {

        public static boolean equals(RuleTerm o1, RuleTerm o2) {
            return o1.getVariable().getName().equals(o2.getVariable().getName())
                    && o1.getTermName().equals(o2.getTermName());
        }

        public static boolean equals(RuleTerm o1, String varName, String termName) {
            return o1.getVariable().getName().equals(varName)
                    && o1.getTermName().equals(termName);
        }

        public static boolean collectionContains(Collection<RuleTerm> col, RuleTerm rt) {
            return col.stream().anyMatch((rtincol) -> (RuleTermComparator.equals(rtincol, rt)));
        }

        public static boolean collectionContains(Collection<RuleTerm> col, String varName, String termName) {
            return col.stream().anyMatch((rtincol) -> (RuleTermComparator.equals(rtincol, varName, termName)));
        }
    }

    public boolean contributesOnlyToGrant(String permission, String varName, String termName) {
        return RuleTermComparator.collectionContains(onlyGrantingInputRuleTerms.get(permission), varName, termName);
    }

    public boolean contributesOnlyToDeny(String permission, String varName, String termName) {
        return RuleTermComparator.collectionContains(onlyDenyingInputRuleTerms.get(permission), varName, termName);
    }

    public Set<String> getPermissions() {
        FunctionBlock fb_ac = this.fis.getFunctionBlock(FuzzyEvaluator.FB_ACCESS_CONTROL_PHASE_NAME);
        HashMap<String, RuleBlock> rb_ac = fb_ac.getRuleBlocks();
        return rb_ac.keySet();
    }
}
