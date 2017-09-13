/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.riac.classifier;

import it.av.fac.messaging.client.DBIRequest;

/**
 *
 * @author Diogo Regateiro
 */
public class RandomClassifier implements IClassifier {
    /**
     * HARDCODED
     */
    private static final String[] CLASSES = {"PUBLIC", "ADMINISTRATIVE", "BUSINESS", "ACADEMIC"};

    @Override
    public void classify(DBIRequest request) {
        request.setMetadata("security_label", CLASSES[(int)(Math.random() * 4)]);
        request.setMetadata("sl_timestamp", String.valueOf(System.currentTimeMillis()));
    }
    
}
