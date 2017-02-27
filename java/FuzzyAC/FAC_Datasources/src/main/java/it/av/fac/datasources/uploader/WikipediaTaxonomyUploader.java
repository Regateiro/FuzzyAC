/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.datasources.uploader;

import com.alibaba.fastjson.JSONArray;
import it.av.fac.driver.APIClient;
import it.av.fac.messaging.client.StorageReply;
import it.av.fac.messaging.client.StorageRequest;
import it.av.fac.messaging.client.StorageRequest.StorageRequestType;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Diogo Regateiro
 */
public class WikipediaTaxonomyUploader {

    public static void main(String[] args) {
        APIClient fac = new APIClient("http://localhost:8084/FAC_Webserver");

        try (BufferedReader in = new BufferedReader(new FileReader("G:\\taxonomy.txt"))) {
            String line;
            while ((line = in.readLine()) != null) {
                String[] catcats = line.split("[:]");
                
                if (catcats.length != 2) {
                    System.out.println("Error in split: " + line);
                    continue;
                }
                        
                StorageRequest request = new StorageRequest();
                request.setRequestType(StorageRequestType.StoreGraphNode);
                request.setStorageId("wct");
                request.setDocument(catcats[0]);
                request.setAditionalInfo("parents", catcats[1]);

                StorageReply reply = fac.storageRequest(request);
            }
        } catch (IOException ex) {
            Logger.getLogger(WikipediaTaxonomyUploader.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
