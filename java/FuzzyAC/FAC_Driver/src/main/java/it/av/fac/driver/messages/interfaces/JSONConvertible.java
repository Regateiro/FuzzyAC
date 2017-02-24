/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.driver.messages.interfaces;

import com.alibaba.fastjson.JSONObject;

/**
 *
 * @author Diogo Regateiro
 */
public interface JSONConvertible {
    public JSONObject toJSONObject();
}
