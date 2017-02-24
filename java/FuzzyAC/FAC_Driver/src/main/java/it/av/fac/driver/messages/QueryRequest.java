/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.driver.messages;

import it.av.fac.driver.messages.interfaces.IRequest;
import it.av.fac.driver.messages.interfaces.JSONConvertible;

/**
 *
 * @author Diogo Regateiro
 */
public class QueryRequest implements IRequest {

    private final QueryRequestType requestType;

    public QueryRequest(QueryRequestType requestType) {
        this.requestType = requestType;
    }
    
    @Override
    public String buildJSON() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setPayload(JSONConvertible jsonConv) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setPayload(String jsonString) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    public enum QueryRequestType {
        ADD_DOCUMENT
    };
}
