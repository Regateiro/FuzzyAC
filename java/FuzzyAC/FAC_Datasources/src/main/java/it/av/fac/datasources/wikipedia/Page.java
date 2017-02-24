/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.datasources.wikipedia;

import com.alibaba.fastjson.JSONObject;
import it.av.fac.driver.messages.interfaces.JSONConvertible;

/**
 *
 * @author Diogo Regateiro
 */
public class Page implements JSONConvertible {

    private boolean redirecting;
    private String title;
    private int id;
    private String text;

    void setRedirecting(boolean redirecting) {
        this.redirecting = redirecting;
    }

    boolean isRedirecting() {
        return this.redirecting;
    }

    void setTitle(String title) {
        this.title = title;
    }

    void setId(int id) {
        this.id = id;
    }

    void setText(String text) {
        this.text = text;
    }

    public String getTitle() {
        return title;
    }

    public int getId() {
        return id;
    }

    public String getText() {
        return text;
    }
    
    @Override
    public JSONObject toJSONObject() {
        JSONObject ret = new JSONObject();
        
        ret.put("redirecting", redirecting);
        ret.put("title", title);
        ret.put("text", text);
        
        return ret;
    }
}
