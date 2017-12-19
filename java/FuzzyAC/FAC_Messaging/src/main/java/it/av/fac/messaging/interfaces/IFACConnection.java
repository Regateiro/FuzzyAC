/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.messaging.interfaces;

import java.io.Closeable;

/**
 * User connection interface to connect to the central messaging node.
 * @author Diogo Regateiro <diogoregateiro@ua.pt>
 */
public interface IFACConnection extends Closeable {
    /**
     * Sends a message.
     * @param message The message to send.
     */
    public void send(byte[] message);
    
    /**
     * Receives a message.
     * @return The message read.
     */
    public byte[] receive();
}
