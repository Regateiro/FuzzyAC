/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.client;

import it.av.fac.driver.APIClient;
import it.av.fac.messaging.client.QueryReply;
import it.av.fac.messaging.client.QueryRequest;

/**
 *
 * @author Regateiro
 */
public class Client {
    public static void main(String[] args) {
        APIClient fac = new APIClient("http://localhost:8084/FAC_Webserver");
        
        QueryRequest request = new QueryRequest();
        request.setRequestType(QueryRequest.QueryRequestType.QueryForDocument);
        request.setQuery("security");
        request.setToken("mytoken");
        request.setTargetData("randwikipages");
        long nanoTime = System.nanoTime();
        QueryReply queryRequest = fac.queryRequest(request);
        nanoTime = System.nanoTime() - nanoTime;
        System.out.println(nanoTime);
    }
}
