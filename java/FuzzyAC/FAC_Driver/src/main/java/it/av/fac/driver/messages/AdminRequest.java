/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.driver.messages;

import com.alibaba.fastjson.JSONObject;
import it.av.fac.driver.messages.interfaces.IRequest;
import it.av.fac.driver.messages.interfaces.JSONConvertible;

/**
 *
 * @author Diogo Regateiro
 */
public class AdminRequest implements IRequest {

    private final AdminRequestType requestType;
    private JSONObject payload;

    public AdminRequest(AdminRequestType requestType) {
        this.requestType = requestType;
    }
    
    @Override
    public String buildJSON() {
        JSONObject ret = new JSONObject();
        
        ret.put("request_type", requestType);
        ret.put("payload", ret.toJSONString());
        
        return ret.toJSONString();
    }

    @Override
    public void setPayload(JSONConvertible jsonConv) {
        this.payload = jsonConv.toJSONObject();
    }

    @Override
    public void setPayload(String jsonString) {
        this.payload = JSONObject.parseObject(jsonString);
    }
    
    public enum AdminRequestType {
        ADD_DOCUMENT
    };
}
