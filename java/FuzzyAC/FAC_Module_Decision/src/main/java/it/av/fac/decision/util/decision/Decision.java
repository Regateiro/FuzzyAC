/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.decision.util.decision;

import it.av.fac.decision.util.variables.Contribution;

/**
 *
 * @author Diogo Regateiro <diogoregateiro@ua.pt>
 */
public enum Decision {
    Granted, Denied;
    
    public boolean matchesContribution(Contribution contrib) {
        return (this == Granted && contrib == Contribution.GRANT) || (this == Denied && contrib == Contribution.DENY);
    }
}
