/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.messaging.interfaces;

import java.io.Closeable;
import java.io.Serializable;

/**
 * User connection interface to connect to the central messaging node.
 * @author Diogo Regateiro
 * @param <S> The type of the message to send.
 * @param <R> The type of the message to receive.
 */
public interface IFACConnection<S extends Serializable, R extends Serializable> extends Closeable {
    /**
     * Sends a message.
     * @param message The message to send.
     */
    public void send(S message);
    
    /**
     * Receives a message.
     * @return The message read.
     */
    public R receive();
}
