/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.decision.fis;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author DiogoJosé
 */
public abstract class AbstractFuzzyAnalyser {

    protected final FuzzyEvaluator feval;
    protected final Map<String, String> outputBuffer;
    protected final Map<String, AtomicInteger> numResults;
    protected String permissionToAnalyse;

    protected AbstractFuzzyAnalyser(FuzzyEvaluator feval) {
        this.feval = feval;
        this.outputBuffer = new HashMap<>();
        this.numResults = new HashMap<>();
    }

    abstract public void analyse(String permission);
}
