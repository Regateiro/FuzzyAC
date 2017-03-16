/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.messaging.client;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import it.av.fac.messaging.client.interfaces.IReply;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.xerial.snappy.Snappy;

/**
 *
 * @author Diogo Regateiro
 */
public class PolicyReply implements IReply<PolicyReply> {

    
    private ReplyStatus status;
    
    private String errorMsg;

    private final List<JSONObject> policies;
    
    public PolicyReply() {
        this.policies = new ArrayList<>();
        this.errorMsg = "";
        this.status = ReplyStatus.OK;
    }

    @Override
    public ReplyStatus getStatus() {
        return status;
    }

    @Override
    public String getErrorMsg() {
        return errorMsg;
    }

    @Override
    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    @Override
    public void setStatus(ReplyStatus status) {
        this.status = status;
    }
    
    public void setPolicies(Collection<JSONObject> policies) {
        this.policies.clear();
        this.policies.addAll(policies);
    }

    public void addPolicy(JSONObject policy) {
        this.policies.add(policy);
    }
    
    public List<JSONObject> getPolicies() {
        return Collections.unmodifiableList(policies);
    }

    @Override
    public byte[] convertToBytes() throws IOException {
        JSONObject ret = new JSONObject();
        
        ret.put("status", status.name());
        ret.put("error_msg", errorMsg);
        
        JSONArray docarray = new JSONArray();
        docarray.addAll(this.policies);
        ret.put("documents", docarray);
        
        return Snappy.compress(ret.toJSONString().getBytes("UTF-8"));
    }

    @Override
    public PolicyReply readFromBytes(byte[] bytes) throws IOException {
        String data = Snappy.uncompressString(bytes, "UTF-8");
        JSONObject obj = JSONObject.parseObject(data);
        
        setStatus(ReplyStatus.valueOf(obj.getString("status")));
        setErrorMsg(obj.getString("error_msg"));
        this.policies.clear();
        
        JSONArray docarray = obj.getJSONArray("documents");
        for(int i = 0; i < docarray.size(); i++) {
            this.policies.add(docarray.getJSONObject(i));
        }
        
        return this;
    }
}
