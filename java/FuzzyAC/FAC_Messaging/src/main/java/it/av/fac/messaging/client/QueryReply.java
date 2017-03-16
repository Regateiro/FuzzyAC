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
import java.util.Collections;
import java.util.List;
import org.xerial.snappy.Snappy;

/**
 *
 * @author Diogo Regateiro
 */
public class QueryReply implements IReply<QueryReply> {

    private ReplyStatus status;
    
    private String errorMsg;
    private List<JSONObject> documents;

    public QueryReply() {
        this.documents = new ArrayList<>();
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
        
        ret.put("status", this.status.name());
        ret.put("error_msg", this.errorMsg);
        
        JSONArray docarray = new JSONArray();
        docarray.addAll(this.documents);
        ret.put("documents", docarray);
        
        return Snappy.compress(ret.toJSONString().getBytes("UTF-8"));
    }

    @Override
    public QueryReply readFromBytes(byte[] bytes) throws IOException {
        String data = Snappy.uncompressString(bytes, "UTF-8");
        JSONObject obj = JSONObject.parseObject(data);
        
        setStatus(ReplyStatus.valueOf(obj.getString("status")));
        setErrorMsg(obj.getString("error_msg"));
        JSONArray docarray = obj.getJSONArray("documents");
        for(int i = 0; i < docarray.size(); i++) {
            addDocument(docarray.getJSONObject(i));
        }
        
        return this;
    }

    public List<JSONObject> getDocuments() {
        return Collections.unmodifiableList(this.documents);
    }
    
    public void addDocument(JSONObject document) {
        this.documents.add(document);
    }
}
