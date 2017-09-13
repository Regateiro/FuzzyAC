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
     * A payload payload to store.
     */
    private String payload;
    
    /**
     * A query to process.
     */
    private String query;
    
    /**
     * The storage identifier.
     */
    private String storageId;
    
    /**
     * Additional payload information.
     */
    private final Map<String, Object> metadata;

    public DBIRequest() {
        this.metadata = new HashMap<>();
    }

    public void setPayload(String document) {
        this.payload = document;
    }
    
    public String getPayload() {
        return this.payload;
    }
    
    public void setQuery(String query) {
        this.query = query;
    }
    
    public String getQuery() {
        return this.query;
    }
    
    public Map<String, Object> getMetadata() {
        return Collections.unmodifiableMap(metadata);
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
    public void setMetadata(String key, String value) {
        this.metadata.put(key, value);
    }

    @Override
    public void setRequestType(DBIRequestType requestType) {
        this.requestType = requestType;
    }

    @Override
    public byte[] convertToBytes() throws IOException {
        JSONObject ret = new JSONObject();
        
        ret.put("request_type", requestType.name());
        ret.put("payload", payload);
        ret.put("storage_id", storageId);
        ret.put("query", query);
        ret.put("metadata", new JSONObject(metadata));
        
        return Snappy.compress(ret.toJSONString().getBytes("UTF-8"));
    }

    @Override
    public DBIRequest readFromBytes(byte[] bytes) throws IOException {
        String data = Snappy.uncompressString(bytes, "UTF-8");
        JSONObject obj = JSONObject.parseObject(data);
        
        setRequestType(DBIRequestType.valueOf(obj.getString("request_type")));
        setPayload(obj.getString("payload"));
        setStorageId(obj.getString("storage_id"));
        setQuery(obj.getString("query"));
        
        this.metadata.clear();
        JSONObject ainfo = obj.getJSONObject("metadata");
        ainfo.keySet().forEach((key) -> {
            setMetadata(key, ainfo.getString(key));
        });
        
        return this;
    }
    
    public enum DBIRequestType implements IRequestType {
        StoreDocument, StoreGraphNode, QueryDocuments, QueryPolicies;
    };
}
