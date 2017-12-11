/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.decision.handlers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates ORES related linguistic term membership degrees.
 * @author Diogo Regateiro
 */
public class ORESCustomMF extends CustomMF {
    private static final String DRAFT_QUALITY = "Draft_Quality";
    private static final String DAMAGING = "Damaging";
    private static final String GOOD_FAITH = "Good_Faith";
    private final List<String> oresUserHistory;

    public ORESCustomMF(List<String> oresUserHistory) {
        this.oresUserHistory = oresUserHistory;
    }
    
    @Override
    protected Map<String, Map<String, Double>> processInternal(Map<String, Object> ciInput) {
        Map<String, Map<String, Double>> ret = new HashMap<>();
        ret.put(DRAFT_QUALITY, new HashMap<>());
        ret.put(DAMAGING, new HashMap<>());
        ret.put(GOOD_FAITH, new HashMap<>());
        
        // use the average of the past month of oresscores
        
        return ret;
    }
}
