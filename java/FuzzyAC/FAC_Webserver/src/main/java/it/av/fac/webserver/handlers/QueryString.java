/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.webserver.handlers;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author DiogoJosé
 */
public class QueryString {

    public static Map<String, String> parseQueryString(String queryString) throws UnsupportedEncodingException {
        Map<String, String> ret = new HashMap<>();

        if (queryString != null) {
            String[] params = queryString.split("[&]");
            for (String param : params) {
                String[] keyvalue = param.split("[=]");
                String key = null, value = null;
                if (keyvalue.length == 2) {
                    key = keyvalue[0];
                    value = URLDecoder.decode(keyvalue[1], "UTF-8");
                } else if (keyvalue.length == 1) {
                    key = keyvalue[0];
                    value = "";
                }

                if (key != null && value != null) {
                    ret.put(key, value);
                }
            }
        }

        return ret;
    }
}
