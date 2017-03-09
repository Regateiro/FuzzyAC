/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.riac.classifier;

import it.av.fac.messaging.client.StorageRequest;

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
    public void classify(StorageRequest request) {
        request.setAditionalInfo("security_label", CLASSES[(int)(Math.random() * 4)]);
    }
    
}
