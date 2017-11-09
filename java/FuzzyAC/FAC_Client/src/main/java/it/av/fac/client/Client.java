/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.client;

import it.av.fac.driver.APIClient;

/**
 *
 * @author Regateiro
 */
public class Client {
    public static void main(String[] args) {
        APIClient fac = new APIClient("http://localhost:8084/FAC_Webserver");
        
        long nanoTime = System.nanoTime();
        String html = fac.queryRequest(null, "Diamond");
        nanoTime = System.nanoTime() - nanoTime;
        System.out.println(nanoTime);
    }
}
