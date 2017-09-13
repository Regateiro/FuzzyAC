/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.dbi.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import it.av.fac.dbi.drivers.DocumentDBI;
import it.av.fac.messaging.client.DBIReply;
import it.av.fac.messaging.client.DBIRequest;
import it.av.fac.messaging.client.ReplyStatus;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 *
 * @author Regateiro
 */
public class PolicyManager {

    private final DocumentDBI client;

    public PolicyManager() throws IOException {
        this.client = DocumentDBI.getInstance("policies");
    }

    public DBIReply getPolicy(String securityLabel) {
        DBIRequest request = new DBIRequest();
        JSONObject fields = new JSONObject();

        JSONArray array = new JSONArray();
        array.add(securityLabel);
        
        fields.put("security_labels", array);
        request.setMetadata("fields", fields.toJSONString());
        return this.client.find(request);
    }

    public DBIReply insertPolicy(String securityLabel, String filePath) {
        DBIRequest request = new DBIRequest();
        DBIReply reply = new DBIReply();
        reply.setStatus(ReplyStatus.OK);

        StringBuilder fcl = new StringBuilder();
        if (filePath != null) {
            try (BufferedReader in = new BufferedReader(new FileReader(filePath))) {
                String line;
                while ((line = in.readLine()) != null) {
                    fcl.append(line).append("\n");
                }
            } catch (Exception ex) {
                reply.setStatus(ReplyStatus.ERROR);
                reply.setErrorMsg(ex.getMessage());
                return reply;
            }
        }

        request.setPayload(filePath != null ? fcl.toString() : null);
        request.setMetadata("security_label", securityLabel);
        this.client.storeDocument(request);
        this.client.syncWithDB();
        return reply;
    }

    public static void main(String[] args) throws IOException {
        PolicyManager pman = new PolicyManager();
        DBIReply publicPol = pman.getPolicy("PUBLIC");
        DBIReply academicPol = pman.getPolicy("ACADEMIC");
        DBIReply businessPol = pman.getPolicy("BUSINESS");
       // DBIReply adminPol = pman.getPolicy("ADMINISTRATIVE");
        System.out.println("");
        //pman.insertPolicy("PUBLIC", null);
        //pman.insertPolicy("ACADEMIC", System.getProperty("user.home") + "\\Documents\\NetBeansProjects\\FuzzyAC\\java\\FuzzyAC\\FAC_Module_Decision\\academic.fcl");
        pman.insertPolicy("ADMINISTRATIVE", System.getProperty("user.home") + "\\Documents\\NetBeansProjects\\FuzzyAC\\java\\FuzzyAC\\FAC_Module_Decision\\administrative.fcl");
        //pman.insertPolicy("BUSINESS", System.getProperty("user.home") + "\\Documents\\NetBeansProjects\\FuzzyAC\\java\\FuzzyAC\\FAC_Module_Decision\\business.fcl");
        //System.out.println(pman.getPolicy("ACADEMIC"));
    }
}
