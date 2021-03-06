/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.dbi.util;

import it.av.fac.dbi.drivers.DocumentDBI;
import it.av.fac.messaging.client.BDFISReply;
import it.av.fac.messaging.client.ReplyStatus;
import it.av.fac.messaging.client.interfaces.IReply;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import org.json.JSONObject;

/**
 *
 * @author Regateiro
 */
public class PolicyManager {

    private final DocumentDBI client;

    public PolicyManager() throws IOException {
        this.client = DocumentDBI.getInstance("policies");
    }

    public IReply getPolicy(String securityLabel) {
        return this.client.findResource(securityLabel);
    }

    public IReply insertPolicy(String securityLabel, String filePath) {
        IReply reply = new BDFISReply();

        StringBuilder fcl = new StringBuilder();
        if (filePath != null) {
            try (BufferedReader in = new BufferedReader(new FileReader(filePath))) {
                String line;
                while ((line = in.readLine()) != null) {
                    fcl.append(line).append("\n");
                }
            } catch (Exception ex) {
                reply = new BDFISReply(ReplyStatus.ERROR, ex.getMessage());
                return reply;
            }
        }

        JSONObject label_fcl = new JSONObject();
        label_fcl.put("_id", securityLabel);
        label_fcl.put("fcl", fcl.toString());

        this.client.storeResource(label_fcl);
        this.client.syncWithDB();
        return reply;
    }

    public static void main(String[] args) throws IOException {
        PolicyManager pman = new PolicyManager();
        System.out.println("Upserting PUBLIC policy: " + pman.insertPolicy("PUBLIC", null).getStatus());
        System.out.println("Upserting ACADEMIC policy: " + pman.insertPolicy("ACADEMIC", args[0] + "academic.fcl").getStatus());
        System.out.println("Upserting ADMINISTRATIVE policy: " + pman.insertPolicy("ADMINISTRATIVE", args[0] + "administrative.fcl").getStatus());
        System.out.println("Upserting BUSINESS policy: " + pman.insertPolicy("BUSINESS", args[0] + "business.fcl").getStatus());
        pman.client.close();
    }
}
