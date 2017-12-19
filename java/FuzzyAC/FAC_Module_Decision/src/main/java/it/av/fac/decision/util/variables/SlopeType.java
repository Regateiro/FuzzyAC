/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.decision.util.variables;

/**
 *
 * @author Diogo Regateiro <diogoregateiro@ua.pt>
 */
public enum SlopeType {
    POSITIVE, NEGATIVE, FLAT, UNKNOWN;
    
    public static SlopeType getSlope(double y1, double y2) {
        return (y1 < y2 ? POSITIVE : (y1 > y2 ? NEGATIVE : FLAT));
    }
}
