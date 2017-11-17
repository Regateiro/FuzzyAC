/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.riac.classifier;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import it.av.fac.messaging.client.interfaces.IRequest;

/**
 *
 * @author Diogo Regateiro
 */
public class HeadingClassifier implements IClassifier {

    /**
     * HARDCODED
     */
    private static final String[] CLASSES = {"PUBLIC", "ACADEMIC", "BUSINESS", "ADMINISTRATIVE"};

    @Override
    public void classify(IRequest request) {
        JSONObject resource = JSONObject.parseObject(request.getResource());
        JSONArray sections = resource.getJSONArray("text");
        for (int i = 0; i < sections.size(); i++) {
            JSONObject section = sections.getJSONObject(i);
            int level = section.getIntValue("level");
            level = Math.min(level - 1, 3);
            section.put("security_label", CLASSES[level]);
            section.put("sl_timestamp", String.valueOf(System.currentTimeMillis()));
            sections.set(i, section);
        }
        resource.put("text", sections);
        request.setResource(resource.toJSONString());
    }

}
