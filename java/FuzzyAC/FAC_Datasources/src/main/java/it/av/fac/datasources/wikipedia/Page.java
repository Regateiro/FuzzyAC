/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.datasources.wikipedia;

import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author Diogo Regateiro <diogoregateiro@ua.pt>
 */
public class Page {

    private boolean redirecting;
    private String title;
    private String text;
    private JSONArray categories;

    public Page() {
    }

    public Page(String jsonPage) {
        JSONObject obj = new JSONObject(jsonPage);
        this.redirecting = obj.getBoolean("redirecting");
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
    
    public String toJSONString() {
        return toJSON().toString();
    }
    
    public JSONObject toJSON() {
        JSONObject ret = new JSONObject();
        
        ret.put("redirecting", redirecting);
        ret.put("title", title);
        ret.put("_id", title);
        ret.put("text", text);
        ret.put("categories", categories);
        
        return ret;
    }
}
