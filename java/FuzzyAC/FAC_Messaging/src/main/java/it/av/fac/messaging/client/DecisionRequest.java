/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.messaging.client;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import it.av.fac.messaging.client.interfaces.IRequest;
import it.av.fac.messaging.client.interfaces.IRequestType;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.xerial.snappy.Snappy;

/**
 *
 * @author Diogo Regateiro
 */
public class DecisionRequest implements IRequest<DecisionRequest, DecisionRequest.DecisionRequestType> {

    /**
     * The type of the request. Used by the system to know what to do.
     */
    private DecisionRequestType requestType;
    
    /**
     * The security labels to make an access decision upon.
     */
    private final Set<String> securityLabels;
    
    /**
     * The token of the user to retrieve the attributes.
     */
    private String userToken;

    public DecisionRequest() {
        this.securityLabels = new HashSet<>();
        this.requestType = DecisionRequestType.Normal;
    }
    
    public void addSecurityLabel(String securityLabel) {
        this.securityLabels.add(securityLabel);
    }
    
    public void setSecurityLabels(Collection<String> securityLabels) {
        this.securityLabels.clear();
        this.securityLabels.addAll(securityLabels);
    }

    public Set<String> getSecurityLabels() {
        return Collections.unmodifiableSet(this.securityLabels);
    }
    
    public void setUserToken(String token) {
        this.userToken = token;
    }
    
    public String getUserToken() {
        return this.userToken;
    }

    @Override
    public byte[] convertToBytes() throws IOException {
        JSONObject ret = new JSONObject();
        
        ret.put("request_type", requestType.name());
        
        JSONArray array = new JSONArray();
        array.addAll(securityLabels);
        ret.put("security_labels", array);
        
        return Snappy.compress(ret.toJSONString().getBytes("UTF-8"));
    }

    @Override
    public DecisionRequest readFromBytes(byte[] bytes) throws IOException {
        String data = Snappy.uncompressString(bytes, "UTF-8");
        JSONObject obj = JSONObject.parseObject(data);

        this.securityLabels.clear();
        setRequestType(DecisionRequestType.valueOf(obj.getString("request_type")));
        obj.getJSONArray("security_labels").stream().forEach((slabel) -> addSecurityLabel((String) slabel));
        
        return this;
    }

    @Override
    public void setRequestType(DecisionRequestType requestType) {
        this.requestType = requestType;
    }

    @Override
    public DecisionRequestType getRequestType() {
        return requestType;
    }
    
    public enum DecisionRequestType implements IRequestType {
        Normal
    };
}