/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.decision.fis;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
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
 * @author Diogo Regateiro <diogoregateiro@ua.pt>
 */
public class VariableDependenceAnalyser {

    private final FIS fis;

    /**
     * Objects that keep the output rule terms that are used for granting/deny
     * permission.
     */
    private final Map<String, Set<SimpleRuleTerm>> grantingOutputRuleTerms;
    private final Map<String, Set<SimpleRuleTerm>> denyingOutputRuleTerms;

    /**
     * Objects that keep the input rule terms that are used for only
     * granting/deny permission.
     */
    private final Map<String, Set<SimpleRuleTerm>> onlyGrantingInputRuleTerms;
    private final Map<String, Set<SimpleRuleTerm>> onlyDenyingInputRuleTerms;

    //A list of variables that appeared in the rulebooks
    private final Set<String> usedVariables;
    private final Set<String> permissions;

    public VariableDependenceAnalyser(FIS fis) {
        this.fis = fis;
        this.grantingOutputRuleTerms = new HashMap<>();
        this.denyingOutputRuleTerms = new HashMap<>();
        this.onlyGrantingInputRuleTerms = new HashMap<>();
        this.onlyDenyingInputRuleTerms = new HashMap<>();
        this.usedVariables = new HashSet<>();
        this.permissions = new HashSet<>();
    }

    public void analyse() {
        //clear variables
        this.grantingOutputRuleTerms.clear();
        this.denyingOutputRuleTerms.clear();
        this.onlyGrantingInputRuleTerms.clear();
        this.onlyDenyingInputRuleTerms.clear();

        //access the AC rulebook and get the associated rules.
        FunctionBlock fb_ac = this.fis.getFunctionBlock(BDFIS.FB_ACCESS_CONTROL_PHASE_NAME);
        HashMap<String, RuleBlock> rb_ac = fb_ac.getRuleBlocks();

        //access the VI rulebook and get the associated rules.
        FunctionBlock fb_vi = this.fis.getFunctionBlock(BDFIS.FB_VARIABLE_INFERENCE_PHASE_NAME);
        HashMap<String, RuleBlock> rb_vi = fb_vi.getRuleBlocks();
        
        // Fill the permissions set
        rb_ac.keySet().forEach((permission) -> {
            this.permissions.add(permission);
        });

        //for each permission
        this.permissions.forEach((permission) -> {
            //Initialize the maps
            grantingOutputRuleTerms.putIfAbsent(permission, new HashSet<>());
            denyingOutputRuleTerms.putIfAbsent(permission, new HashSet<>());
            onlyGrantingInputRuleTerms.putIfAbsent(permission, new HashSet<>());
            onlyDenyingInputRuleTerms.putIfAbsent(permission, new HashSet<>());

            //for each rule in the rulebook
            rb_ac.get(permission).getRules().forEach((Rule rule) -> {
                Set<SimpleRuleTerm> ruleTerms = new HashSet<>();

                //obtain all the rule terms from the rule into r_rts
                flattenRuleExpression(rule.getAntecedents(), ruleTerms);

                if (rule.getConsequents().size() > 1) {
                    System.err.println("More than one consequence in a rule. Using only the first one.");
                }

                //check if the rule results into a grant or deny and adds the rule terms to the revelant list.
                RuleTerm consequent = rule.getConsequents().getFirst();
                if (consequent.getLinguisticTerm().getTermName().equalsIgnoreCase("grant")) {
                    grantingOutputRuleTerms.get(permission).addAll(ruleTerms);
                } else {
                    denyingOutputRuleTerms.get(permission).addAll(ruleTerms);
                }
            });

            //for each rulebook
            rb_vi.keySet().forEach((String abstractVariable) -> {
                RuleBlock rb = rb_vi.get(abstractVariable);
                rb.getRules().forEach((Rule rule) -> {
                    Set<SimpleRuleTerm> ruleTerms = new HashSet<>();

                    //obtain all the rule terms from the rule into r_rts
                    flattenRuleExpression(rule.getAntecedents(), ruleTerms);

                    if (rule.getConsequents().size() > 1) {
                        System.err.println("more than one consequence in a rule. Using only the first one.");
                    }

                    //check if the consequent of the rule is in the granting set or the denying set and add it to the appropriate set.
                    SimpleRuleTerm consequent = new SimpleRuleTerm(rule.getConsequents().getFirst());
                    if (grantingOutputRuleTerms.get(permission).contains(consequent) && !denyingOutputRuleTerms.get(permission).contains(consequent)) {
                        onlyGrantingInputRuleTerms.get(permission).addAll(ruleTerms);
                    }

                    if (!grantingOutputRuleTerms.get(permission).contains(consequent) && denyingOutputRuleTerms.get(permission).contains(consequent)) {
                        onlyDenyingInputRuleTerms.get(permission).addAll(ruleTerms);
                    }
                });
            });

            //add used variables into the used variables set
            onlyGrantingInputRuleTerms.get(permission).stream().map((ruleTerm) -> ruleTerm.getVariableName()).forEach((varName) -> usedVariables.add(varName));
            onlyDenyingInputRuleTerms.get(permission).stream().map((ruleTerm) -> ruleTerm.getVariableName()).forEach((varName) -> usedVariables.add(varName));
        });
    }

    private void flattenRuleExpression(RuleExpression expression, Set<SimpleRuleTerm> flatList) {
        Object[] terms = new Object[]{expression.getTerm1(), expression.getTerm2()};

        for (Object term : terms) {
            if (expression.isFuzzyRuleExpression(term)) {
                flattenRuleExpression((RuleExpression) term, flatList);
            } else if (expression.isFuzzyRuleTerm(term)) {
                flatList.add(new SimpleRuleTerm((RuleTerm) term));
            }
        }
    }

    public boolean variableIsUsed(String varName) {
        return usedVariables.contains(varName);
    }

    public boolean contributesOnlyToGrant(String permission, String varName, String termName) {
        return onlyGrantingInputRuleTerms.get(permission).contains(new SimpleRuleTerm(varName, termName));
    }

    public boolean contributesOnlyToDeny(String permission, String varName, String termName) {
        return onlyDenyingInputRuleTerms.get(permission).contains(new SimpleRuleTerm(varName, termName));
    }

    public Set<String> getPermissions() {
        return Collections.unmodifiableSet(this.permissions);
    }

    private class SimpleRuleTerm {

        private final String variableName;
        private final String termName;

        public SimpleRuleTerm(RuleTerm rt) {
            this.variableName = rt.getVariable().getName();
            this.termName = rt.getTermName();
        }

        public SimpleRuleTerm(String variableName, String termName) {
            this.variableName = variableName;
            this.termName = termName;
        }

        public String getVariableName() {
            return this.variableName;
        }

        public String getTermName() {
            return this.termName;
        }

        @Override
        public String toString() {
            return this.variableName + "[" + this.termName + "]";
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 71 * hash + Objects.hashCode(this.variableName);
            hash = 71 * hash + Objects.hashCode(this.termName);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final SimpleRuleTerm other = (SimpleRuleTerm) obj;
            return this.variableName.equalsIgnoreCase(other.variableName) && this.termName.equalsIgnoreCase(other.termName);
        }
    }
}
