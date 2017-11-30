/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.messaging.client.interfaces;

import it.av.fac.messaging.client.RequestType;
import java.io.IOException;

/**
 * Interface for the Request objects.
 * @author Diogo Regateiro
 */
public interface IRequest {
    public String getUserToken();
    public Object getResourceId();
    public RequestType getRequestType();
    public byte[] convertToBytes() throws IOException;
    public String getResource();
    public void setResource(String resource);
}
