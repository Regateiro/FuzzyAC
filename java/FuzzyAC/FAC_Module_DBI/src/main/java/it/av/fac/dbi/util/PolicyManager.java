/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.dbi.util;

import com.alibaba.fastjson.JSONObject;
import it.av.fac.dbi.drivers.DocumentDBI;
import it.av.fac.messaging.client.BDFISReply;
import it.av.fac.messaging.client.BDFISRequest;
import it.av.fac.messaging.client.ReplyStatus;
import it.av.fac.messaging.client.RequestType;
import it.av.fac.messaging.client.interfaces.IReply;
import it.av.fac.messaging.client.interfaces.IRequest;
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

    public IReply getPolicy(String securityLabel) {
        IRequest request = new BDFISRequest(null, securityLabel, RequestType.GetPolicy);
        return this.client.findPolicy(request);
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
        label_fcl.put("security_label", securityLabel);
        label_fcl.put("fcl", fcl);

        this.client.storeResource(label_fcl);
        this.client.syncWithDB();
        return reply;
    }

    public static void main(String[] args) throws IOException {
        PolicyManager pman = new PolicyManager();
        IReply publicPol = pman.getPolicy("PUBLIC");
        IReply academicPol = pman.getPolicy("ACADEMIC");
        IReply businessPol = pman.getPolicy("BUSINESS");
        // DBIReply adminPol = pman.getPolicy("ADMINISTRATIVE");
        System.out.println("");
        //pman.insertPolicy("PUBLIC", null);
        //pman.insertPolicy("ACADEMIC", System.getProperty("user.home") + "\\Documents\\NetBeansProjects\\FuzzyAC\\java\\FuzzyAC\\FAC_Module_Decision\\academic.fcl");
        pman.insertPolicy("ADMINISTRATIVE", System.getProperty("user.home") + "\\Documents\\NetBeansProjects\\FuzzyAC\\java\\FuzzyAC\\FAC_Module_Decision\\administrative.fcl");
        //pman.insertPolicy("BUSINESS", System.getProperty("user.home") + "\\Documents\\NetBeansProjects\\FuzzyAC\\java\\FuzzyAC\\FAC_Module_Decision\\business.fcl");
        //System.out.println(pman.getPolicy("ACADEMIC"));
    }
}
