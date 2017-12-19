/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.riac.ingestion.wikipedia;

import it.av.fac.riac.classifier.IClassifier;

/**
 * Reads and converts XML files from the Wikipedia dumps, classifying them.
 * @author Diogo Regateiro <diogoregateiro@ua.pt>
 */
public class WikiXMLReader extends Thread {

    private final IClassifier classifier;
    private final String xml;

    public WikiXMLReader(IClassifier classifier, String xml) {
        this.classifier = classifier;
        this.xml = xml;
    }
    
    @Override
    public void run() {
        
    }
    
}
