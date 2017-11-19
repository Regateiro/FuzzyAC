/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.decision.util.handlers;

import it.av.fac.decision.fis.AbstractFuzzyAnalyser;
import it.av.fac.decision.util.decision.Decision;
import it.av.fac.decision.util.decision.DecisionResult;
import java.util.List;

/**
 *
 * @author Diogo Regateiro
 */
public class ResultHandlerToConsole implements IResultHandler {

    private final AbstractFuzzyAnalyser.DecisionResultsToReturn decisionsToHandle;

    public ResultHandlerToConsole(AbstractFuzzyAnalyser.DecisionResultsToReturn decisionsToHandle) {
        this.decisionsToHandle = decisionsToHandle;
    }

    @Override
    public void handleResults(List<DecisionResult> results) {
        results.stream().forEachOrdered((result) -> handleSingleResult(result));
    }

    @Override
    public void handleSingleResult(DecisionResult result) {
        switch (decisionsToHandle) {
            case ALL:
                System.out.println(result);
                break;
            case ONLY_GRANT:
                if (result.getDecision() == Decision.Granted) {
                    System.out.println(result);
                }
                break;
            case ONLY_DENY:
                if (result.getDecision() == Decision.Denied) {
                    System.out.println(result);
                }
                break;
        }
    }

    @Override
    public void close() {
    }
}
