/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.decision.util;

/**
 * Updates a value between two limits, moving up from the minimum value to the
 * max and then down once the max value is reached. Helps with making sure that
 * only one variable at a tie is updated and only by a step of 1;
 *
 * @author DiogoJosé
 */
public class RangeValue {

    private final String varName;
    private String termName;
    private double slope;
    private int max;
    private int min;
    private int currVal;
    private int direction;

    public RangeValue(String varName, int min, int max) {
        this.varName = varName;
        this.max = max;
        this.min = min;
        this.currVal = min;
        this.direction = 1;
    }
    
    public RangeValue(String varName, String termName, double slope, int min, int max) {
        this.varName = varName;
        this.termName = termName;
        this.slope = slope;
        this.max = max;
        this.min = min;
        this.currVal = min;
        this.direction = 1;
    }

    public int next() {
        currVal = currVal + direction;
        return currVal;
    }

    public boolean isOnTheEdge() {
        return currVal + direction < min || currVal + direction > max;
    }

    public String getVarName() {
        return varName;
    }

    public String getTermName() {
        return termName;
    }
    
    public int getCurrentValue() {
        return currVal;
    }

    @Override
    public String toString() {
        return String.format("%d", currVal);
    }

    public void invertDirection() {
        direction *= -1;
    }

    public double getSlope() {
        return slope;
    }
    
    public boolean mergeIfOverlaps(RangeValue rv) {
        if ((rv.min < this.max && rv.max >= this.max) || (this.min < rv.max && this.max >= rv.max)) {
            this.min = Math.min(rv.min, this.min);
            this.max = Math.max(rv.max, this.max);
            return true;
        }

        return false;
    }
}
