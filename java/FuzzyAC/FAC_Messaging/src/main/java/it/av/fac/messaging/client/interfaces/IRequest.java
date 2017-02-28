/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.messaging.client.interfaces;

import java.io.IOException;

/**
 * Interface for the Request objects.
 * @author Diogo Regateiro
 * @param <R>
 * @param <T>
 */
public interface IRequest<R extends IRequest, T extends IRequestType> {
    public void setRequestType(T requestType);
    public byte[] convertToBytes() throws IOException;
    public R readFromBytes(byte[] bytes) throws IOException;
}
