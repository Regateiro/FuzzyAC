/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.decision.util.variables;

import java.util.ArrayList;
import java.util.Collection;
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
    private SlopeType slopeType;
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

    public RangeValue(String varName, SlopeType slopeType, int min, int max) {
        this.varName = varName;
        this.slopeType = slopeType;
        this.max = max;
        this.min = min;
        this.currVal = min;
        this.direction = 1;
    }

    public RangeValue(RangeValue rv, int min, int max) {
        this.varName = rv.varName;
        this.slopeType = rv.slopeType;
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

    public int getCurrentValue() {
        return currVal;
    }

    @Override
    public String toString() {
        return String.format("(%d-%d; %d [%s])", min, max, currVal, contribution);
    }

    public void invertDirection() {
        direction *= -1;
    }

    public SlopeType getSlopeType() {
        return slopeType;
    }

    public void setContribution(boolean contributesToGrant, boolean contributesToDeny) {
        if (contributesToGrant && !contributesToDeny) {
            switch (slopeType) {
                case POSITIVE:
                    contribution = Contribution.GRANT;
                    break;
                case NEGATIVE:
                    contribution = Contribution.DENY;
                    break;
                case FLAT:
                    contribution = Contribution.NONE;
                    break;
                case UNKNOWN:
                    contribution = Contribution.UNKNOWN;
            }
        } else if (!contributesToGrant && contributesToDeny) {
            switch (slopeType) {
                case POSITIVE:
                    contribution = Contribution.DENY;
                    break;
                case NEGATIVE:
                    contribution = Contribution.GRANT;
                    break;
                case FLAT:
                    contribution = Contribution.NONE;
                    break;
                case UNKNOWN:
                    contribution = Contribution.UNKNOWN;
            }
        } else {
            contribution = Contribution.UNKNOWN;
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

        int thisXmin = this.getMin();
        int thisXmax = this.getMax();
        int otherXmin = otherRange.getMin();
        int otherXmax = otherRange.getMax();

        int commonXmin = Math.max(thisXmin, otherXmin);
        int commonXmax = Math.min(thisXmax, otherXmax);

        // if both ranges have the same contribution, just merge them both
        if (this.getContribution() == otherRange.getContribution()) {
            resultingRanges.add(new RangeValue(this, Math.min(thisXmin, otherXmin), Math.max(thisXmax, otherXmax)));
        } else {
            //contributions differ, process ranges outside the overlap
            if (thisXmin < commonXmin) {
                resultingRanges.add(new RangeValue(this, thisXmin, commonXmin));
            } else if (otherXmin < commonXmin) {
                resultingRanges.add(new RangeValue(otherRange, otherXmin, commonXmin));
            }

            if (thisXmax > commonXmax) {
                resultingRanges.add(new RangeValue(this, commonXmax, thisXmax));
            } else if (otherXmax > commonXmax) {
                resultingRanges.add(new RangeValue(otherRange, commonXmax, otherXmax));
            }

            //process the overlapping range
            //if the slope in any of them has no contribution, use the other
            if (this.getContribution() == Contribution.NONE) {
                resultingRanges.add(new RangeValue(otherRange, commonXmin, commonXmax));
            } else if (otherRange.getContribution() == Contribution.NONE) {
                resultingRanges.add(new RangeValue(this, commonXmin, commonXmax));
            } else {
                // they both differ in contribution and both contribute to at last one decision, then the variable contribution on this range is unknown.
                RangeValue rv = new RangeValue(this, commonXmin, commonXmax);
                rv.slopeType = SlopeType.UNKNOWN;
                rv.contribution = Contribution.UNKNOWN;
                resultingRanges.add(rv);
            }
        }

        return resultingRanges;
    }

    public Contribution getContribution() {
        if(this.direction == -1) {
            if(this.contribution == Contribution.GRANT) {
                return Contribution.DENY;
            } else if(this.contribution == Contribution.DENY) {
                return Contribution.GRANT;
            }
        }
        
        return contribution;
    }

    void setToMin() {
        this.currVal = min;
        this.direction = 1;
    }
    
    void setToMax() {
        this.currVal = max;
        this.direction = -1;
    }

    public boolean canMergeWith(RangeValue otherRange) {
        return getVarName().equalsIgnoreCase(otherRange.getVarName()) && getContribution() == otherRange.getContribution();
    }

    public RangeValue mergeWith(RangeValue otherRange) {
        return new RangeValue(this, Math.min(min, otherRange.getMin()), Math.max(max, otherRange.getMax()));
    }
}
