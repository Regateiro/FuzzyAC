/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.messaging.client;

import com.alibaba.fastjson.JSONObject;
import it.av.fac.messaging.client.interfaces.IReply;
import java.io.IOException;
import org.xerial.snappy.Snappy;

/**
 *
 * @author Diogo Regateiro
 */
public class InformationReply implements IReply<InformationReply> {

    private ReplyStatus status;
    
    private String errorMsg;

    public InformationReply() {
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

    @Override
    public byte[] convertToBytes() throws IOException {
        JSONObject ret = new JSONObject();
        
        ret.put("status", status.name());
        ret.put("error_msg", errorMsg);
        
        return Snappy.compress(ret.toJSONString().getBytes("UTF-8"));
    }

    @Override
    public InformationReply readFromBytes(byte[] bytes) throws IOException {
        String data = Snappy.uncompressString(bytes, "UTF-8");
        JSONObject obj = JSONObject.parseObject(data);
        
        setStatus(ReplyStatus.valueOf(obj.getString("status")));
        setErrorMsg(obj.getString("error_msg"));
        
        return this;
    }
}