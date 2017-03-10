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
import java.util.ArrayList;
import java.util.List;
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
     * The request to 
     */
    private final List<String> securityLabels;

    public DecisionRequest() {
        this.securityLabels = new ArrayList<>();
    }
    
    public void addSecurityLabel(String securityLabel) {
        this.securityLabels.add(securityLabel);
    }
    
    public void setSecurityLabels(List<String> securityLabels) {
        this.securityLabels.addAll(securityLabels);
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
    
    public enum DecisionRequestType implements IRequestType {
        FOR_READING
    };
}
