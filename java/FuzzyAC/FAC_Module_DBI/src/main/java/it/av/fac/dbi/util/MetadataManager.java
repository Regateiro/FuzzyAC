/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.dbi.util;

import com.alibaba.fastjson.JSONObject;
import it.av.fac.dbi.drivers.DocumentDBI;
import java.io.IOException;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.codec.binary.Base64;
import org.bson.Document;
import org.xerial.snappy.Snappy;

/**
 *
 * @author Regateiro
 */
public class MetadataManager {
    public static void main(String[] args) throws IOException {
        DocumentDBI client = DocumentDBI.getInstance("metadata");
        client.processResources(new Consumer<Document>() {
            @Override
            public void accept(Document t) {
                try {
                    JSONObject resource = JSONObject.parseObject(t.toJson());
                    
                    String wikipage = Snappy.uncompressString(Base64.decodeBase64(resource.getString("text")));
                    resource.put("text", WikiParser.clean(resource.getString("title"), wikipage));
                    
                    client.updateResource(resource, "text");
                } catch (IOException ex) {
                    Logger.getLogger(MetadataManager.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
    }
}
