/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.decision.util.handlers;

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

    public ResultHandlerToFile(String filePath) throws FileNotFoundException {
        this.handlerOut = new PrintWriter(filePath);
    }

    @Override
    public void handleResults(List<DecisionResult> results) {
        results.stream().forEachOrdered((result) -> this.handlerOut.println(result));
    }

    @Override
    public void handleSingleResult(DecisionResult result) {
        this.handlerOut.println(result);
    }

    @Override
    public void close() {
        this.handlerOut.flush();
        this.handlerOut.close();
    }

}
