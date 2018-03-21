/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.dfcl2;

import java.util.List;
import java.util.Map;

/**
 *
 * @author Regateiro
 */
public interface ESource extends IExternalService {

    public Map<String, Double> process(Map<String, Double> inputValues);

    public String getSourceLabel();
    
    public List<String> getInputVariableLabels();
}
