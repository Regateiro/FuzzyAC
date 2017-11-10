/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.messaging.client;

import com.alibaba.fastjson.JSONObject;
import it.av.fac.messaging.client.interfaces.IRequest;
import java.io.IOException;
import org.xerial.snappy.Snappy;

/**
 *
 * @author Diogo Regateiro
 */
public class BDFISRequest implements IRequest {

    /**
     * The userToken which identifies the user in the system. Used for caching.
     */
    private final String userToken;

    /**
     * The id of the resource to query from.
     */
    private final String resourceId;
    
    /**
     * The resource to add/store.
     */
    private String resource;

    /**
     * The type of the resource to fetch.
     */
    private final RequestType requestType;

    public BDFISRequest(String userToken, String resourceId, RequestType requestType) {
        this.userToken = userToken;
        this.resourceId = resourceId;
        this.requestType = requestType;
    }

    @Override
    public String getUserToken() {
        return userToken;
    }

    @Override
    public String getResourceId() {
        return resourceId;
    }

    @Override
    public RequestType getRequestType() {
        return requestType;
    }

    @Override
    public byte[] convertToBytes() throws IOException {
        JSONObject ret = new JSONObject();

        ret.put("userToken", userToken);
        ret.put("resource_id", resourceId);
        ret.put("resource", resource);
        ret.put("requesttype", requestType);

        return Snappy.compress(ret.toJSONString());
    }

    public static BDFISRequest readFromBytes(byte[] bytes) throws IOException {
        String data = Snappy.uncompressString(bytes, "UTF-8");
        JSONObject obj = JSONObject.parseObject(data);

        BDFISRequest request = new BDFISRequest(
                obj.getString("userToken"),
                obj.getString("resource_id"),
                RequestType.valueOf(obj.getString("requesttype"))
        );
        request.setResource(obj.getString("resource"));

        return request;
    }

    @Override
    public String getResource() {
        return resource;
    }

    @Override
    public void setResource(String resource) {
        this.resource = resource;
    }
}
