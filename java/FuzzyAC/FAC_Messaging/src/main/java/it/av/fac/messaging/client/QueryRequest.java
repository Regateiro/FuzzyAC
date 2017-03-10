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
import java.util.Collections;
import java.util.Map;
import org.xerial.snappy.Snappy;

/**
 *
 * @author Diogo Regateiro
 */
public class QueryRequest implements IRequest<QueryRequest, QueryRequest.QueryRequestType> {

    /**
     * The type of the request. Used by the system to know what to do.
     */
    private QueryRequestType requestType;
    
    /**
     * The query to run on the datastore.
     */
    private String query;
    
    /**
     * The token which identifies the user in the system. Used for caching.
     */
    private String token;


    public QueryRequest() {
    }
    
    public void setQuery(String query) {
        this.query = query;
    }
    
    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public void setRequestType(QueryRequestType requestType) {
        this.requestType = requestType;
    }

    public QueryRequestType getRequestType() {
        return requestType;
    }

    public String getQuery() {
        return query;
    }

    public String getToken() {
        return token;
    }
    
    @Override
    public byte[] convertToBytes() throws IOException {
        JSONObject ret = new JSONObject();
        
        ret.put("request_type", requestType.name());
        ret.put("query", query);
        ret.put("token", token);
        
        return Snappy.compress(ret.toJSONString());
    }

    @Override
    public QueryRequest readFromBytes(byte[] bytes) throws IOException {
        String data = Snappy.uncompressString(bytes, "UTF-8");
        JSONObject obj = JSONObject.parseObject(data);
        
        setRequestType(QueryRequestType.valueOf(obj.getString("request_type")));
        setQuery(obj.getString("query"));
        setToken(obj.getString("token"));
        
        return this;
    }
    
    public enum QueryRequestType implements IRequestType {
        QueryForDocument
    };
}
