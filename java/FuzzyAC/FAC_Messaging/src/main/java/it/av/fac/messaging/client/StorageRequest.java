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
import java.util.HashMap;
import java.util.Map;
import org.xerial.snappy.Snappy;

/**
 *
 * @author Diogo Regateiro
 */
public class StorageRequest implements IRequest<StorageRequest, StorageRequest.StorageRequestType> {

    /**
     * The type of the request. Used by the system to know what to do.
     */
    private StorageRequestType requestType;
    
    /**
     * A document payload to store.
     */
    private String document;
    
    /**
     * The storage identifier.
     */
    private String storageId;
    
    /**
     * Additional document information.
     */
    private final Map<String, Object> aditionalInfo;

    public StorageRequest() {
        this.aditionalInfo = new HashMap<>();
    }

    public void setDocument(String document) {
        this.document = document;
    }
    
    public String getDocument() {
        return this.document;
    }

    public String getAditionalInfo(String key) {
        return (String) aditionalInfo.get(key);
    }

    public StorageRequestType getRequestType() {
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
    public void setRequestType(StorageRequestType requestType) {
        this.requestType = requestType;
    }

    @Override
    public byte[] convertToBytes() throws IOException {
        JSONObject ret = new JSONObject();
        
        ret.put("request_type", requestType.name().toLowerCase());
        ret.put("document", document);
        ret.put("storage_id", storageId);
        ret.put("aditional_info", new JSONObject(aditionalInfo));
        
        return Snappy.compress(ret.toJSONString().getBytes("UTF-8"));
    }

    @Override
    public StorageRequest readFromBytes(byte[] bytes) throws IOException {
        String data = Snappy.uncompressString(bytes, "UTF-8");
        JSONObject obj = JSONObject.parseObject(data);
        
        setRequestType(StorageRequestType.valueOf(obj.getString("request_type")));
        setDocument(obj.getString("document"));
        setStorageId(obj.getString("storage_id"));
        
        this.aditionalInfo.clear();
        JSONObject ainfo = obj.getJSONObject("aditional_info");
        ainfo.keySet().forEach((key) -> {
            setAditionalInfo(key, ainfo.getString(key));
        });
        
        return this;
    }
    
    public enum StorageRequestType implements IRequestType {
        StoreDocument, StoreGraphNode;
    };
}
