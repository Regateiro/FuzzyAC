/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.decision.util.handlers;

import it.av.fac.decision.util.decision.DecisionResult;
import java.util.ArrayDeque;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;

/**
 * Can be used to validate if two sets of results are exactly the same.
 *
 * @author Diogo Regateiro
 */
public class ValidatorHandler implements IResultHandler {

    private final Queue<DecisionResult> queuedResults;
    private boolean validate;
    private boolean validationResult;
    private boolean isEnabled;

    public ValidatorHandler() {
        this.queuedResults = new ArrayDeque<>();
        this.validate = false;
        this.validationResult = true;
        this.isEnabled = true;
    }

    @Override
    public void handleResults(List<DecisionResult> results) {
        if (isEnabled) {
            results.stream().forEachOrdered((result) -> handleSingleResult(result));
        }
    }

    @Override
    public void handleSingleResult(DecisionResult result) {
        if (isEnabled) {
            if (validate) {
                try {
                    DecisionResult other = queuedResults.remove();
                    if (!other.equals(result)) {
                        validationResult = false;
                    }
                } catch (NoSuchElementException ex) {
                    validationResult = false;
                }
            } else {
                queuedResults.add(result);
            }
        }
    }

    @Override
    public void close() {
        this.queuedResults.clear();
    }

    public void disableHandler() {
        this.isEnabled = false;
    }

    public void enableHandler() {
        this.isEnabled = true;
    }

    public void enableValidation() {
        this.validate = true;
    }

    public boolean wasValidationSuccessul() {
        return this.validationResult && queuedResults.isEmpty();
    }

    public void reset() {
        this.queuedResults.clear();
        this.validationResult = true;
        this.validate = false;
        this.isEnabled = true;
    }
}
