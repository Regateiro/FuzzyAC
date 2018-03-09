/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.dfcl2;

import java.util.Map;

/**
 * External Input Fuzzifier Service (EIFS)
 *
 * @author Diogo Regateiro <diogoregateiro@ua.pt>
 */
public interface EIFS extends IExternalService {

    public Map<String, Double> process(double inputValue);

    public String getInputVariableLabel();
}
