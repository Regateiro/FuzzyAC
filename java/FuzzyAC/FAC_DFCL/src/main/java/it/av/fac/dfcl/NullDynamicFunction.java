/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.dfcl;

import java.util.Map;

/**
 *
 * @author Diogo Regateiro <diogoregateiro@ua.pt>
 */
public class NullDynamicFunction extends DynamicFunction {
    @Override
    protected Map<String, Map<String, Double>> processInternal() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
