/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.webserver.handlers;

import com.alibaba.fastjson.JSONObject;

/**
 * Interface for the Web request Handler objects.
 * @author Diogo Regateiro
 */
public interface Handler {
    public JSONObject handle(JSONObject request);
}
