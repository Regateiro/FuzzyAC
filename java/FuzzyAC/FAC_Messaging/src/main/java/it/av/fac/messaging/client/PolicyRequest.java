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
public class PolicyRequest implements IRequest<PolicyRequest, PolicyRequest.PolicyRetrievalRequestType> {

    /**
     * The type of the request. Used by the system to know what to do.
     */
    private PolicyRetrievalRequestType requestType;
    
    /**
     * The query to run on the datastore.
     */
    private Set<String> securityLabels;

    public PolicyRequest() {
        securityLabels = new HashSet<>();
    }

    @Override
    public PolicyRetrievalRequestType getRequestType() {
        return requestType;
    }

    public Set<String> getSecurityLabels() {
        return Collections.unmodifiableSet(securityLabels);
    }
    
    public void addSecurityLabel(String securityLabel) {
        this.securityLabels.add(securityLabel);
    }
    
    public void setSecurityLabels(Collection<String> securityLabels) {
        this.securityLabels.clear();
        this.securityLabels.addAll(securityLabels);
    }

    @Override
    public byte[] convertToBytes() throws IOException {
        JSONObject ret = new JSONObject();
        
        ret.put("request_type", requestType.name());
        
        JSONArray labels = new JSONArray();
        labels.addAll(securityLabels);
        ret.put("security_labels", labels);
        
        return Snappy.compress(ret.toJSONString().getBytes("UTF-8"));
    }

    @Override
    public PolicyRequest readFromBytes(byte[] bytes) throws IOException {
        String data = Snappy.uncompressString(bytes, "UTF-8");
        JSONObject obj = JSONObject.parseObject(data);
        
        setRequestType(PolicyRetrievalRequestType.valueOf(obj.getString("request_type")));
        
        this.securityLabels.clear();
        obj.getJSONArray("security_labels").stream().forEach((slabel) -> addSecurityLabel((String) slabel));
        
        return this;
    }

    @Override
    public void setRequestType(PolicyRetrievalRequestType requestType) {
        this.requestType = requestType;
    }
    
    public enum PolicyRetrievalRequestType implements IRequestType {
        BySecurityLabel
    };
}
