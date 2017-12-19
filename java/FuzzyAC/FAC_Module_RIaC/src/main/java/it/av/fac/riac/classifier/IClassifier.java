/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.riac.classifier;

import it.av.fac.messaging.client.interfaces.IRequest;

/**
 * Interface for the classifier classes.
 * @author Diogo Regateiro <diogoregateiro@ua.pt>
 */
public interface IClassifier {
    public void classify(IRequest request);
}
