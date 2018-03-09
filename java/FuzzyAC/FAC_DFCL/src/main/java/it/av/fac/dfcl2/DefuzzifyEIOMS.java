/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.dfcl2;

import java.util.Collection;
import net.sourceforge.jFuzzyLogic.rule.Variable;

/**
 * External Input Output Mapping Service (EIOMS) with defuzzification.
 * @author Diogo Regateiro <diogoregateiro@ua.pt>
 */
public interface DefuzzifyEIOMS extends EIOMS {
    public double process(Collection<Variable> inputVector);
}
