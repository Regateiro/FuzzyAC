/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.messaging.client;

import com.alibaba.fastjson.JSONObject;
import it.av.fac.messaging.client.interfaces.IRequest;
import it.av.fac.messaging.client.interfaces.IRequestType;
import java.io.IOException;
import org.xerial.snappy.Snappy;

/**
 *
 * @author Diogo Regateiro
 */
public class InformationRequest implements IRequest<InformationRequest, InformationRequest.InformationRequestType> {

    /**
     * The type of the request. Used by the system to know what to do.
     */
    private InformationRequestType requestType;
    
    /**
     * The query to run on the datastore.
     */
    private String query;

    public InformationRequest() {
    }
    
    public void setQuery(String query) {
        this.query = query;
    }

    @Override
    public byte[] convertToBytes() throws IOException {
        JSONObject ret = new JSONObject();
        
        ret.put("request_type", requestType.name());
        ret.put("query", query);
        
        return Snappy.compress(ret.toJSONString().getBytes("UTF-8"));
    }

    @Override
    public InformationRequest readFromBytes(byte[] bytes) throws IOException {
        String data = Snappy.uncompressString(bytes, "UTF-8");
        JSONObject obj = JSONObject.parseObject(data);
        
        setRequestType(InformationRequestType.valueOf(obj.getString("request_type")));
        setQuery(obj.getString("query"));
        
        return this;
    }

    @Override
    public void setRequestType(InformationRequestType requestType) {
        this.requestType = requestType;
    }
    
    public enum InformationRequestType implements IRequestType {
    };
}
