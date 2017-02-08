/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.messaging.interfaces.client;

import java.io.Serializable;

/**
 * User connection interface to connect to the central messaging node.
 * @author Diogo Regateiro
 * @param <T> The type of the message to send and receive.
 */
public interface IFACConnection<T extends Serializable> {
    /**
     * Sends a message.
     * @param message The message to send.
     */
    public void send(T message);
    
    /**
     * Receives a message.
     * @return The message read.
     */
    public T receive();
}
