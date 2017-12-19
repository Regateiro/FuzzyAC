/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.messaging.interfaces;

/**
 * An interface to handle messages.
 * @author Diogo Regateiro <diogoregateiro@ua.pt>
 * @param <R> The type of the message to handle.
 */
public interface IClientHandler<R> {
    public void handle(R message);
}
