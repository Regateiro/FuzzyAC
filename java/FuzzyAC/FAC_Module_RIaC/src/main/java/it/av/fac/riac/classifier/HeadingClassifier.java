/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.riac.classifier;

import it.av.fac.messaging.client.interfaces.IRequest;
import org.json.JSONArray;
import org.json.JSONObject;

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
        JSONObject resource = new JSONObject(request.getResource());
        JSONArray sections = resource.getJSONArray("text");
        for (int i = 0; i < sections.length(); i++) {
            JSONObject section = sections.getJSONObject(i);
            int level = section.getInt("level");
            level = Math.min(level - 1, 3);
            section.put("security_label", CLASSES[level]);
            section.put("sl_timestamp", String.valueOf(System.currentTimeMillis()));
            sections.put(i, section);
        }
        resource.put("text", sections);
        request.setResource(resource.toString());
    }

}
