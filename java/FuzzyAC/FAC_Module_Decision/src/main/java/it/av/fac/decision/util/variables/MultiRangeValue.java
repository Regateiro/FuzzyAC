/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.decision.util.variables;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author DiogoJosé
 */
public class MultiRangeValue {

    private final List<RangeValue> ranges;
    private int idx;
    private int direction;

    public MultiRangeValue(List<RangeValue> ranges) {
        this.ranges = ranges;
        this.idx = 0;
        this.direction = 1;
    }

    public boolean isOnTheEdge() {
        return (idx + direction < 0 || idx + direction == ranges.size()) && ranges.get(idx).isOnTheEdge();
    }

    public void invertDirection() {
        ranges.get(idx).invertDirection();
        direction *= -1;
    }

    public int next() {
        //If the variable is on the edge
        if (ranges.get(idx).isOnTheEdge()) {
            ranges.get(idx).invertDirection();

            //Is there another variable after this one?
            if (idx + direction >= 0 && idx + direction < ranges.size()) {
                // if so move to it.
                idx += direction;
            }

            return ranges.get(idx).getCurrentValue();
        } else {
            return ranges.get(idx).next();
        }
    }

    public String getVarName() {
        return ranges.get(idx).getVarName();
    }

    public int getCurrentValue() {
        return ranges.get(idx).getCurrentValue();
    }

    public Contribution getContribution() {
        return ranges.get(idx).getContribution();
    }

    public Contribution getNextValueContribution() {
        if (ranges.get(idx).isOnTheEdge() && idx + direction >= 0 && idx + direction < ranges.size()) {
            return ranges.get(idx + direction).getContribution();
        }

        return ranges.get(idx).getContribution();
    }
    
    public int getContributionRangeSize(Contribution contrib) {
        AtomicInteger ret = new AtomicInteger();
        ranges.parallelStream().forEach((range) -> {
            if (range.getContribution() == contrib) {
                ret.addAndGet(range.getMax() - range.getMin() + 1);
            }
        });
        return ret.get();
    }

    @Override
    public String toString() {
        final StringBuilder ret = new StringBuilder(this.getVarName());
        ranges.stream().forEachOrdered((range) -> ret.append("\n\t").append(range.toString()));
        ret.append("\n");
        return ret.toString();
    }

    public void setToMin() {
        this.idx = 0;
        this.direction = 1;
        this.ranges.parallelStream().forEach((range) -> range.setToMin());
    }

    public void setToMax() {
        this.idx = this.ranges.size() - 1;
        this.direction = -1;
        this.ranges.parallelStream().forEach((range) -> range.setToMax());
    }
    
    public double getMin() {
        return this.ranges.get(0).getMin();
    }
    
    public double getMax() {
        return this.ranges.get(this.ranges.size() - 1).getMax();
    }

    public boolean isSetToMin() {
        return getCurrentValue() == this.ranges.get(0).getMin();
    }
    
    public boolean isSetToMax() {
        return getCurrentValue() == this.ranges.get(this.ranges.size() - 1).getMax();
    }
    
    public double getRangeSize() {
        AtomicInteger ret = new AtomicInteger();
        ranges.parallelStream().forEach((range) -> {
            ret.addAndGet(range.getMax() - range.getMin() + 1);
        });
        return ret.get();
    }
}
