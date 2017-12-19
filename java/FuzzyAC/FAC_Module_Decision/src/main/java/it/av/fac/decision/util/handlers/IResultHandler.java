/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.decision.util.handlers;

import it.av.fac.decision.util.decision.DecisionResult;
import java.io.Closeable;
import java.util.List;

/**
 *
 * @author Diogo Regateiro <diogoregateiro@ua.pt>
 */
public interface IResultHandler extends Closeable {
    public void handleResults(List<DecisionResult> results);
    public void handleSingleResult(DecisionResult result);
    @Override
    public void close();
}
