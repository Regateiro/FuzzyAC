/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.decision.fis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.sourceforge.jFuzzyLogic.FIS;
import net.sourceforge.jFuzzyLogic.FunctionBlock;
import net.sourceforge.jFuzzyLogic.rule.LinguisticTerm;
import net.sourceforge.jFuzzyLogic.rule.RuleBlock;

/**
 * Variable Dependence Analyser.
 * @author Diogo Regateiro
 */
public class VDA {
    private final FIS fis;
    private final Map<String, List<LinguisticTerm>> _Perm_OutputVarLTs;
    private final Map<LinguisticTerm, List<LinguisticTerm>> _O_InputVarLTs;

    public VDA(FIS fis) {
        this.fis = fis;
        this._Perm_OutputVarLTs = new HashMap<>();
        this._O_InputVarLTs = new HashMap<>();
    }
    
    public void analyse() {
        //access the AC rulebook and get the associated rules.
        FunctionBlock fb_ac = this.fis.getFunctionBlock(FuzzyEvaluator.FB_ACCESS_CONTROL_PHASE_NAME);
        HashMap<String, RuleBlock> rb_ac = fb_ac.getRuleBlocks();
        rb_ac.keySet().stream().forEach((permission) -> {
            RuleBlock rb = rb_ac.get(permission);
            rb.getRules().stream().forEach((rule) -> {
                rule.getConsequents().stream().map((ruleterm) -> ruleterm.getLinguisticTerm()).forEach(lt -> {
                    _Perm_OutputVarLTs.putIfAbsent(permission, new ArrayList<>());
                    _Perm_OutputVarLTs.get(permission).add(lt);
                });
            });
        });
        
        System.out.println("");
    }
}
