/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.dfcl2;

import java.util.Collection;
import java.util.Map;
import net.sourceforge.jFuzzyLogic.rule.Variable;

/**
 * External Input Output Mapping Service (EIOMS) without defuzzification.
 * @author Diogo Regateiro <diogoregateiro@ua.pt>
 */
public interface NoDefuzzifyEIOMS extends EIOMS {
    public Map<String, Double> process(Collection<Variable> inputVector);
}
