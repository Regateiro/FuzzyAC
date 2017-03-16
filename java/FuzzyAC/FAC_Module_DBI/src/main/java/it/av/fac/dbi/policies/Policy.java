/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.dbi.policies;

import org.bson.Document;

/**
 *
 * @author Regateiro
 */
public class Policy {

    private final String securityLabel;
    private final String fcl;

    public Policy(String securityLabel, String fcl) {
        this.securityLabel = securityLabel;
        this.fcl = fcl;
    }
    
    public Document toMongoDocument() {
        Document ret = new Document();
        ret.append("security_label", securityLabel);
        ret.append("document", fcl);
        return ret;
    }

    @Override
    public String toString() {
        return "Policy{" + "securityLabel=" + securityLabel + ", fcl=" + fcl + '}';
    }
}
