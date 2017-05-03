/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.decision.util;

import it.av.fac.decision.fis.Contribution;
import java.util.ArrayList;
import java.util.List;

/**
 * Updates a value between two limits, moving up from the minimum value to the
 * max and then down once the max value is reached. Helps with making sure that
 * only one variable at a tie is updated and only by a step of 1;
 *
 * @author Diogo Regateiro
 */
public class RangeValue {

    private final String varName;
    private int max;
    private int min;
    private String termName;
    private double slope;
    private double b;
    private int currVal;
    private int direction;
    private Contribution contribution;

    public RangeValue(String varName, int min, int max) {
        this.varName = varName;
        this.max = max;
        this.min = min;
        this.currVal = min;
        this.direction = 1;
    }

    public RangeValue(String varName, String termName, double slope, double b, int min, int max) {
        this.varName = varName;
        this.termName = termName;
        this.slope = slope;
        this.max = max;
        this.min = min;
        this.currVal = min;
        this.direction = 1;
        this.b = b;
    }

    public RangeValue(RangeValue rv, int min, int max) {
        this.varName = rv.varName;
        this.termName = rv.termName;
        this.slope = rv.slope;
        this.max = max;
        this.min = min;
        this.currVal = min;
        this.direction = 1;
        this.b = rv.b;
        this.contribution = rv.contribution;
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
        return String.format("(%d-%d;%d)", min, max, currVal);
    }

    public void invertDirection() {
        direction *= -1;
    }

    public double getSlope() {
        return slope;
    }

    public void setContribution(boolean contributesToGrant, boolean contributesToDeny) {
        if (contributesToGrant && !contributesToDeny) {
            if (slope > 0) {
                contribution = Contribution.Grant;
            }
            if (slope < 0) {
                contribution = Contribution.Deny;
            }
            if (slope == 0) {
                contribution = Contribution.None;
            }
        } else if (!contributesToGrant && contributesToDeny) {
            if (slope > 0) {
                contribution = Contribution.Deny;
            }
            if (slope < 0) {
                contribution = Contribution.Grant;
            }
            if (slope == 0) {
                contribution = Contribution.None;
            }
        } else {
            contribution = Contribution.Unknown;
        }
    }

    public boolean overlapsWith(RangeValue rv) {
        return (rv.min < this.max && rv.max >= this.max) || (this.min < rv.max && this.max >= rv.max);
    }

    public int getMax() {
        return max;
    }

    public int getMin() {
        return min;
    }

    public double getB() {
        return b;
    }

    public List<RangeValue> splitFrom(RangeValue rv) {
        List<RangeValue> extraRanges = new ArrayList<>();

        //calculate the intersect x
        double commonMin = Math.max(this.min, rv.min);
        double commonMax = Math.min(this.max, rv.max);

        double rv1y1 = this.slope * commonMin + this.b;
        double rv1y2 = this.slope * commonMax + this.b;
        double rv2y1 = rv.slope * commonMin + rv.b;
        double rv2y2 = rv.slope * commonMax + rv.b;

        if (rv1y1 > rv2y1 && rv1y2 > rv2y2) {
            // they don't intersect, this is always greater
            // push the other range past this range.
            rv.min = (int) commonMax;
            rv.currVal = rv.min;
        } else if (rv1y1 < rv2y1 && rv1y2 < rv2y2) {
            // they don't intersect, this is always smaller
            // pull the this range before the other.
            this.max = (int) commonMin;
        } else {
            // they intersect, so calculate that point
            double intersect = (rv.b - this.b) / (this.slope - rv.slope);

            if (rv1y1 < rv2y1 && rv1y2 > rv2y2) {
                // this intersects from below
                if (this.min < commonMin) {
                    extraRanges.add(new RangeValue(this, this.min, (int) commonMin));
                }
                if (rv.max > commonMax) {
                    extraRanges.add(new RangeValue(rv, (int) commonMax, rv.max));
                }
                this.min = (int) Math.ceil(intersect);
                this.currVal = this.min;
                rv.max = (int) Math.floor(intersect);
            } else if (rv1y1 > rv2y1 && rv1y2 < rv2y2) {
                // this intersects from above
                if (rv.min < commonMin) {
                    extraRanges.add(new RangeValue(rv, rv.min, (int) commonMin));
                }
                if (this.max > commonMax) {
                    extraRanges.add(new RangeValue(this, (int) commonMax, this.max));
                }
                rv.min = (int) Math.ceil(intersect);
                rv.currVal = rv.min;
                this.max = (int) Math.floor(intersect);
            }
        }

        return extraRanges;
    }

    public Contribution getContribution() {
        return contribution;
    }
}
