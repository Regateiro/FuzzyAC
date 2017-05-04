/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.decision.util;

/**
 *
 * @author Diogo Regateiro
 */
public class LinearFunction {

    private final double m;
    private final double b;

    public LinearFunction(double x1, double x2, double y1, double y2) {
        this.m = (y2 - y1) / (x2 - x1);
        this.b = (x2 * y1 - x1 * y2) / (x2 - x1);
    }

    public double getConstant() {
        return b;
    }

    public double getSlope() {
        return m;
    }

    public double getY(double x) {
        return m * x + b;
    }
    
    public Double getIntersect(LinearFunction other) {
        if(this.m - other.m == 0) return null;
        return (other.b - this.b) / (this.m - other.m);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final LinearFunction other = (LinearFunction) obj;
        if (Double.doubleToLongBits(this.m) != Double.doubleToLongBits(other.m)) {
            return false;
        }
        if (Double.doubleToLongBits(this.b) != Double.doubleToLongBits(other.b)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + (int) (Double.doubleToLongBits(this.m) ^ (Double.doubleToLongBits(this.m) >>> 32));
        hash = 97 * hash + (int) (Double.doubleToLongBits(this.b) ^ (Double.doubleToLongBits(this.b) >>> 32));
        return hash;
    }

    @Override
    public String toString() {
        return String.format("[y = %fx + %f]", m, b);
    }
}
