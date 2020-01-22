/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.decision.util.handlers;

import it.av.fac.decision.util.decision.DecisionResult;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;

/**
 * Can be used to validate if two sets of results are exactly the same.
 *
 * @author Diogo Regateiro <diogoregateiro@ua.pt>
 */
public class ValidatorHandler implements IResultHandler {

    private final Queue<DecisionResult> queuedResults;
    private PrintWriter out;
    private boolean validate;
    private boolean validationResult;
    private boolean isEnabled;

    public ValidatorHandler() {
        this.queuedResults = new ArrayDeque<>();
        this.validate = false;
        this.validationResult = true;
        this.isEnabled = true;
    }

    public void setOutputFile(String filePath) throws FileNotFoundException {
        File outFile = new File(filePath);
        this.out = new PrintWriter(new FileOutputStream(outFile), true);
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
                    System.out.print(String.format(" --> Validating... %09d results left.\r", queuedResults.size()));
                    if (!other.equals(result)) {
                        validationResult = false;
                    }
                } catch (NoSuchElementException ex) {
                    validationResult = false;
                }
            } else {
                queuedResults.add(result);
                System.out.print(String.format(" --> Analysing... %09d results.\r", queuedResults.size()));
                if (out != null) {
                    out.println(result);
                }
            }
        }
    }

    @Override
    public void close() {
        this.queuedResults.clear();
        this.validationResult = true;
        this.validate = false;
        this.isEnabled = true;
        if (this.out != null) {
            this.out.flush();
            this.out.close();
            this.out = null;
        }
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
}
