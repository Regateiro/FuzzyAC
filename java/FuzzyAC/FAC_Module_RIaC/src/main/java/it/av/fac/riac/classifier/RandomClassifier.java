/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.riac.classifier;

import com.alibaba.fastjson.JSONObject;
import it.av.fac.messaging.client.interfaces.IRequest;


/**
 *
 * @author Diogo Regateiro
 */
public class RandomClassifier implements IClassifier {
    /**
     * HARDCODED
     */
    private static final String[] CLASSES = {"PUBLIC", "ADMINISTRATIVE", "BUSINESS", "ACADEMIC"};

    @Override
    public void classify(IRequest request) {
        JSONObject resource = JSONObject.parseObject(request.getResource());
        resource.put("security_label", CLASSES[(int)(Math.random() * 4)]);
        resource.put("sl_timestamp", String.valueOf(System.currentTimeMillis()));
        request.setResource(resource.toJSONString());
    }
    
}
