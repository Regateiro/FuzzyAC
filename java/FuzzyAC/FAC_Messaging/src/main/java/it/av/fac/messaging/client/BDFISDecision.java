/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.messaging.client;

import com.alibaba.fastjson.JSONObject;
import java.io.IOException;

/**
 *
 * @author Diogo Regateiro
 */
public class BDFISDecision {
    private final String securityLabel;
    private final String permission;
    private final boolean granted;

    public BDFISDecision(String securityLabel, String permission, boolean granted) {
        this.securityLabel = securityLabel;
        this.permission = permission;
        this.granted = granted;
    }

    public String getSecurityLabel() {
        return securityLabel;
    }

    public String getPermission() {
        return permission;
    }

    public boolean isGranted() {
        return granted;
    }
    
    public String convertToString() {
        JSONObject ret = new JSONObject();

        ret.put("security_label", securityLabel);
        ret.put("permission", permission);
        ret.put("granted", granted);

        return ret.toJSONString();
    }

    public static BDFISDecision readFromString(String str) {
        JSONObject obj = JSONObject.parseObject(str);

        BDFISDecision decision = new BDFISDecision(
                obj.getString("security_label"),
                obj.getString("permission"),
                Boolean.valueOf(obj.getString("granted"))
        );

        return decision;
    }
}