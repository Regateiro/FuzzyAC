/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.driver;

import it.av.fac.driver.messages.AdminRequest;
import it.av.fac.driver.messages.AdminReply;
import it.av.fac.driver.messages.QueryRequest;
import it.av.fac.driver.messages.QueryReply;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

/**
 *
 * @author Diogo Regateiro
 */
public class APIClient {

    private final static String REQUEST_PATH = "/RequestAPI";
    private final static String ADMIN_PATH = "/AdminAPI";
    private final String endpoint;

    public APIClient(String endpoint) {
        this.endpoint = endpoint;
    }

    public QueryReply queryRequest(QueryRequest request) {
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(endpoint + REQUEST_PATH);

            List<NameValuePair> nvps = new ArrayList<>();
            nvps.add(new BasicNameValuePair("request", request.buildJSON()));
            httpPost.setEntity(new UrlEncodedFormEntity(nvps));

            // Create a custom response handler
            ResponseHandler<String> responseHandler = (final HttpResponse response) -> {
                int status = response.getStatusLine().getStatusCode();
                if (status >= 200 && status < 300) {
                    HttpEntity entity = response.getEntity();
                    return entity != null ? EntityUtils.toString(entity) : null;
                } else {
                    throw new ClientProtocolException("Unexpected response status: " + status);
                }
            };

            return new QueryReply(httpclient.execute(httpPost, responseHandler));
        } catch (IOException ex) {
            Logger.getLogger(APIClient.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
    
    public AdminReply adminRequest(AdminRequest request) {
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(endpoint + ADMIN_PATH);

            List<NameValuePair> nvps = new ArrayList<>();
            nvps.add(new BasicNameValuePair("request", request.buildJSON()));
            httpPost.setEntity(new UrlEncodedFormEntity(nvps));

            // Create a custom response handler
            ResponseHandler<String> responseHandler = (final HttpResponse response) -> {
                int status = response.getStatusLine().getStatusCode();
                if (status >= 200 && status < 300) {
                    HttpEntity entity = response.getEntity();
                    return entity != null ? EntityUtils.toString(entity) : null;
                } else {
                    throw new ClientProtocolException("Unexpected response status: " + status);
                }
            };

            return new AdminReply(httpclient.execute(httpPost, responseHandler));
        } catch (IOException ex) {
            Logger.getLogger(APIClient.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
}
