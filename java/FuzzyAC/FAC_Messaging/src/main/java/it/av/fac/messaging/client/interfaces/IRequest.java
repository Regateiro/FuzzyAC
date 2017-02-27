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
 */
public interface IRequest {
    public void setRequestType(IRequestType requestType);
    public byte[] convertToBytes() throws IOException;
    public IRequest readFromBytes(byte[] bytes) throws IOException;
}
