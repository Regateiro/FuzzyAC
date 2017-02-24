/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.driver.messages.interfaces;

/**
 * Interface for the Request objects.
 * @author Diogo Regateiro
 */
public interface IRequest {
    public void setPayload(JSONConvertible jsonConv);
    public void setPayload(String jsonConv);
    public String buildJSON();
}
