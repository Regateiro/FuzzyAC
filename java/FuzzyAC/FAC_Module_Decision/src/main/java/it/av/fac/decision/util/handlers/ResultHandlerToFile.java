/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.decision.util.handlers;

import it.av.fac.decision.fis.AbstractFuzzyAnalyser;
import it.av.fac.decision.util.decision.Decision;
import it.av.fac.decision.util.decision.DecisionResult;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.List;

/**
 *
 * @author Diogo Regateiro
 */
public class ResultHandlerToFile implements IResultHandler {

    private final PrintWriter handlerOut;
    private final AbstractFuzzyAnalyser.DecisionResultsToReturn decisionsToHandle;

    public ResultHandlerToFile(String filePath, AbstractFuzzyAnalyser.DecisionResultsToReturn decisionsToHandle) throws FileNotFoundException {
        this.handlerOut = new PrintWriter(filePath);
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
                this.handlerOut.println(result);
                break;
            case ONLY_GRANT:
                if (result.getDecision() == Decision.Granted) {
                    this.handlerOut.println(result);
                }
                break;
            case ONLY_DENY:
                if (result.getDecision() == Decision.Denied) {
                    this.handlerOut.println(result);
                }
                break;
        }
    }

    @Override
    public void close() {
        this.handlerOut.flush();
        this.handlerOut.close();
    }

}
