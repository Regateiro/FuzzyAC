/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.datasources.wikipedia;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import java.util.List;

/**
 *
 * @author Diogo Regateiro
 */
public class Page {

    private boolean redirecting;
    private String title;
    private String text;
    private JSONArray categories;

    public Page() {
    }

    public Page(String jsonPage) {
        JSONObject obj = JSONObject.parseObject(jsonPage);
        this.redirecting = obj.getBooleanValue("redirecting");
        this.title = obj.getString("title");
        this.text = obj.getString("text");
        this.categories = obj.getJSONArray("categories");
    }
    
    public void setRedirecting(boolean redirecting) {
        this.redirecting = redirecting;
    }

    public boolean isRedirecting() {
        return this.redirecting;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getTitle() {
        return title;
    }

    public String getText() {
        return text;
    }
    
    public void setCategories(List categories) {
        this.categories = new JSONArray(categories);
    }
    
    public JSONArray getCategories() {
        return this.categories;
    }
}
