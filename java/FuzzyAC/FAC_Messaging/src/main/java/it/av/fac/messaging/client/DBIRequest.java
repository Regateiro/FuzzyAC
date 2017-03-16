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
import java.util.HashMap;
import java.util.Map;
import org.xerial.snappy.Snappy;

/**
 *
 * @author Diogo Regateiro
 */
public class DBIRequest implements IRequest<DBIRequest, DBIRequest.DBIRequestType> {

    /**
     * The type of the request. Used by the system to know what to do.
     */
    private DBIRequestType requestType;
    
    /**
     * A document payload to store.
     */
    private String document;
    
    /**
     * A query to process.
     */
    private String query;
    
    /**
     * The storage identifier.
     */
    private String storageId;
    
    /**
     * Additional document information.
     */
    private final Map<String, Object> aditionalInfo;

    public DBIRequest() {
        this.aditionalInfo = new HashMap<>();
    }

    public void setDocument(String document) {
        this.document = document;
    }
    
    public String getDocument() {
        return this.document;
    }
    
    public void setQuery(String query) {
        this.query = query;
    }
    
    public String getQuery() {
        return this.query;
    }
    
    public Map<String, Object> getAditionalInfo() {
        return Collections.unmodifiableMap(aditionalInfo);
    }

    @Override
    public DBIRequestType getRequestType() {
        return requestType;
    }

    public String getStorageId() {
        return storageId;
    }

    public void setStorageId(String storageId) {
        this.storageId = storageId;
    }
    
    /**
     * Object should override toString().
     * @param key
     * @param value 
     */
    public void setAditionalInfo(String key, String value) {
        this.aditionalInfo.put(key, value);
    }

    @Override
    public void setRequestType(DBIRequestType requestType) {
        this.requestType = requestType;
    }

    @Override
    public byte[] convertToBytes() throws IOException {
        JSONObject ret = new JSONObject();
        
        ret.put("request_type", requestType.name());
        ret.put("document", document);
        ret.put("storage_id", storageId);
        ret.put("query", query);
        ret.put("aditional_info", new JSONObject(aditionalInfo));
        
        return Snappy.compress(ret.toJSONString().getBytes("UTF-8"));
    }

    @Override
    public DBIRequest readFromBytes(byte[] bytes) throws IOException {
        String data = Snappy.uncompressString(bytes, "UTF-8");
        JSONObject obj = JSONObject.parseObject(data);
        
        setRequestType(DBIRequestType.valueOf(obj.getString("request_type")));
        setDocument(obj.getString("document"));
        setStorageId(obj.getString("storage_id"));
        setQuery(obj.getString("query"));
        
        this.aditionalInfo.clear();
        JSONObject ainfo = obj.getJSONObject("aditional_info");
        ainfo.keySet().forEach((key) -> {
            setAditionalInfo(key, ainfo.getString(key));
        });
        
        return this;
    }
    
    public enum DBIRequestType implements IRequestType {
        StoreDocument, StoreGraphNode, QueryDocuments, QueryPolicies;
    };
}