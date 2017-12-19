/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.decision.util.handlers;

import it.av.fac.decision.util.decision.DecisionResult;
import java.util.List;

/**
 * Does nothing with the results.
 * @author Diogo Regateiro <diogoregateiro@ua.pt>
 */
public class NullHandler implements IResultHandler {

    @Override
    public void handleResults(List<DecisionResult> results) {
    }

    @Override
    public void handleSingleResult(DecisionResult result) {
    }

    @Override
    public void close() {
    }
    
}
