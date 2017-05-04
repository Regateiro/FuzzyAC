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
    private final int max;
    private final int min;
    private String termName;
    private LinearFunction function;
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

    public RangeValue(String varName, String termName, LinearFunction function, int min, int max) {
        this.varName = varName;
        this.termName = termName;
        this.function = function;
        this.max = max;
        this.min = min;
        this.currVal = min;
        this.direction = 1;
    }

    public RangeValue(RangeValue rv, int min, int max) {
        this.varName = rv.varName;
        this.termName = rv.termName;
        this.function = rv.function;
        this.max = max;
        this.min = min;
        this.currVal = min;
        this.direction = 1;
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
        return String.format("(%d-%d; %d)", min, max, currVal);
    }

    public void invertDirection() {
        direction *= -1;
    }

    public LinearFunction getFunction() {
        return function;
    }

    public void setContribution(boolean contributesToGrant, boolean contributesToDeny) {
        double slope = function.getSlope();

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

    public List<RangeValue> splitFrom(RangeValue otherRange) {
        List<RangeValue> resultingRanges = new ArrayList<>();

        //calculate the intersect x
        double commonMin = Math.max(this.min, otherRange.min);
        double commonMax = Math.min(this.max, otherRange.max);

        double thisYleft = this.getFunction().getY(commonMin);
        double thisYright = this.getFunction().getY(commonMax);
        double otherYleft = otherRange.getFunction().getY(commonMin);
        double otherYright = otherRange.getFunction().getY(commonMax);

        if (thisYleft > otherYleft && thisYright > otherYright) {
            // they don't intersect, this is always greater
            // push the other range before and after this range.
            if (otherRange.min < commonMin) {
                resultingRanges.add(new RangeValue(otherRange, otherRange.min, (int) commonMin));
            }

            if (otherRange.max > commonMax) {
                resultingRanges.add(new RangeValue(otherRange, (int) commonMax, otherRange.max));
            }

            resultingRanges.add(this);
        } else if (thisYleft < otherYleft && thisYright < otherYright) {
            // they don't intersect, this is always smaller
            // pull the this range before and after the other.
            if (this.min < commonMin) {
                resultingRanges.add(new RangeValue(this, this.min, (int) commonMin));
            }

            if (this.max > commonMax) {
                resultingRanges.add(new RangeValue(this, (int) commonMax, this.max));
            }

            resultingRanges.add(otherRange);
        } else {
            // they intersect, so calculate that point
            Double intersect = function.getIntersect(otherRange.getFunction());
            
            if (thisYleft < otherYleft && thisYright > otherYright) {
                // this intersects from below
                // other should take the left of intersect, this to the right.
                // If this has part of a function before the commonmin, it should be saved. Same with the other if it goes past the commonmax.
                if (this.min < commonMin) {
                    resultingRanges.add(new RangeValue(this, this.min, (int) commonMin));
                }
                if (otherRange.max > commonMax) {
                    resultingRanges.add(new RangeValue(otherRange, (int) commonMax, otherRange.max));
                }
                resultingRanges.add(new RangeValue(otherRange, otherRange.min, (int) Math.round(intersect)));
                resultingRanges.add(new RangeValue(this, (int) Math.round(intersect), this.max));

            } else if (thisYleft > otherYleft && thisYright < otherYright) {
                // this intersects from above
                // other should take the right of intersect, this to the left.
                // If other has part of a function before the commonmin, it should be saved. Same with this if it goes past the commonmax.
                if (otherRange.min < commonMin) {
                    resultingRanges.add(new RangeValue(otherRange, otherRange.min, (int) commonMin));
                }
                if (this.max > commonMax) {
                    resultingRanges.add(new RangeValue(this, (int) commonMax, this.max));
                }
                resultingRanges.add(new RangeValue(this, this.min, (int) Math.round(intersect)));
                resultingRanges.add(new RangeValue(otherRange, (int) Math.round(intersect), otherRange.max));
            }
        }

        return resultingRanges;
    }

    public Contribution getContribution() {
        return contribution;
    }
}
