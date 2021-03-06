/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.driver;

import java.io.IOException;
import java.net.URLEncoder;
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
import org.json.JSONObject;

/**
 *
 * @author Diogo Regateiro <diogoregateiro@ua.pt>
 */
public class APIClient {

    private final static String QUERY_PATH = "/WikiAPI";
    private final static String ADMIN_PATH = "/WikiAdminAPI";
    private final String endpoint;

    public APIClient(String endpoint) {
        this.endpoint = endpoint;
    }

    public String queryRequest(String userToken, String resource) {
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(endpoint + QUERY_PATH);

            List<NameValuePair> nvps = new ArrayList<>();
            nvps.add(new BasicNameValuePair("request", URLEncoder.encode(resource, "UTF-8")));
            nvps.add(new BasicNameValuePair("token", URLEncoder.encode(userToken, "UTF-8")));
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

            return httpclient.execute(httpPost, responseHandler);
        } catch (IOException ex) {
            Logger.getLogger(APIClient.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
    
    public boolean storageRequest(String userToken, JSONObject resource) {
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(endpoint + ADMIN_PATH);

            List<NameValuePair> nvps = new ArrayList<>();
            nvps.add(new BasicNameValuePair("resource", URLEncoder.encode(resource.toString(), "UTF-8")));
            nvps.add(new BasicNameValuePair("token", URLEncoder.encode(userToken, "UTF-8")));
            httpPost.setEntity(new UrlEncodedFormEntity(nvps));

            // Create a custom response handler
            ResponseHandler<Boolean> responseHandler = (final HttpResponse response) -> {
                int status = response.getStatusLine().getStatusCode();
                if (status >= 200 && status < 300) {
                    return status == 200;
                } else {
                    throw new ClientProtocolException("Unexpected response status: " + status);
                }
            };

            return httpclient.execute(httpPost, responseHandler);
        } catch (IOException ex) {
            Logger.getLogger(APIClient.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }
}
