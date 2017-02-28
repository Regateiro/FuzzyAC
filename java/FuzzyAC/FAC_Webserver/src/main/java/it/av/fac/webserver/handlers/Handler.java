/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.webserver.handlers;

import it.av.fac.messaging.client.interfaces.IReply;
import it.av.fac.messaging.client.interfaces.IRequest;

/**
 * Interface for the Web request Handler objects.
 * @author Diogo Regateiro
 * @param <R>
 * @param <P>
 */
public interface Handler<R extends IRequest, P extends IReply> {
    public P handle(R request);
}
