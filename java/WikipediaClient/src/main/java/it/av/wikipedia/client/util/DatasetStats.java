/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.wikipedia.client.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author Diogo Regateiro <diogoregateiro@ua.pt>
 */
public class DatasetStats {
    public static void main(String[] args) throws IOException {
        int numUsers = 0, numRevisions = 0, numNextRevisions = 0;
        try(BufferedReader in = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream("complete_dataset.gz"))))) {
            String line;
            while((line = in.readLine()) != null) {
                JSONObject user = new JSONObject(line);
                numUsers++;
                
                JSONArray revs = user.getJSONArray("revisions");
                numRevisions += revs.length();
                
                for(int i = 0; i < revs.length(); i++) {
                    JSONObject rev = revs.getJSONObject(i);
                    numNextRevisions += rev.getJSONArray("next_revisions").length();
                }
            }
        }
        
        System.out.println("Number of users: " + numUsers);
        System.out.println("Number of revisions: " + numRevisions);
        System.out.println("Number of next revisions: " + numNextRevisions);
    }
}
