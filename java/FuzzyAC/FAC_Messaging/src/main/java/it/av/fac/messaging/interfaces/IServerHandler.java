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
 * @param <S> The type of the object with the sender information.
 */
public interface IServerHandler<R, S> {
    public void handle(R message, S sender);
}
