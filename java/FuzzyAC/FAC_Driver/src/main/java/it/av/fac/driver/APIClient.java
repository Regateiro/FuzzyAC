/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.driver;

import it.av.fac.messaging.client.AdminReply;
import it.av.fac.messaging.client.AdminRequest;
import it.av.fac.messaging.client.DBIRequest;
import it.av.fac.messaging.client.DBIReply;
import it.av.fac.messaging.client.QueryRequest;
import it.av.fac.messaging.client.QueryReply;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.codec.binary.Base64;
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

    private final static String QUERY_PATH = "/QueryAPI";
    private final static String ADMIN_PATH = "/AdminAPI";
    private final static String STORAGE_PATH = "/StorageAPI";
    private final String endpoint;

    public APIClient(String endpoint) {
        this.endpoint = endpoint;
    }

    public QueryReply queryRequest(QueryRequest request) {
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(endpoint + QUERY_PATH);

            List<NameValuePair> nvps = new ArrayList<>();
            nvps.add(new BasicNameValuePair("request", Base64.encodeBase64URLSafeString(request.convertToBytes())));
            httpPost.setEntity(new UrlEncodedFormEntity(nvps));

            // Create a custom response handler
            ResponseHandler<byte[]> responseHandler = (final HttpResponse response) -> {
                int status = response.getStatusLine().getStatusCode();
                if (status >= 200 && status < 300) {
                    HttpEntity entity = response.getEntity();
                    return entity != null ? Base64.decodeBase64(EntityUtils.toString(entity)) : null;
                } else {
                    throw new ClientProtocolException("Unexpected response status: " + status);
                }
            };

            return (QueryReply) new QueryReply().readFromBytes(httpclient.execute(httpPost, responseHandler));
        } catch (IOException ex) {
            Logger.getLogger(APIClient.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
    
    public DBIReply storageRequest(DBIRequest request) {
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(endpoint + STORAGE_PATH);

            List<NameValuePair> nvps = new ArrayList<>();
            nvps.add(new BasicNameValuePair("request", Base64.encodeBase64URLSafeString(request.convertToBytes())));
            httpPost.setEntity(new UrlEncodedFormEntity(nvps));

            // Create a custom response handler
            ResponseHandler<byte[]> responseHandler = (final HttpResponse response) -> {
                int status = response.getStatusLine().getStatusCode();
                if (status >= 200 && status < 300) {
                    HttpEntity entity = response.getEntity();
                    return entity != null ? Base64.decodeBase64(EntityUtils.toString(entity)) : null;
                } else {
                    throw new ClientProtocolException("Unexpected response status: " + status);
                }
            };

            return (DBIReply) new DBIReply().readFromBytes(httpclient.execute(httpPost, responseHandler));
        } catch (IOException ex) {
            Logger.getLogger(APIClient.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
    
    public AdminReply adminRequest(AdminRequest request) {
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(endpoint + STORAGE_PATH);

            List<NameValuePair> nvps = new ArrayList<>();
            nvps.add(new BasicNameValuePair("request", Base64.encodeBase64URLSafeString(request.convertToBytes())));
            httpPost.setEntity(new UrlEncodedFormEntity(nvps));

            // Create a custom response handler
            ResponseHandler<byte[]> responseHandler = (final HttpResponse response) -> {
                int status = response.getStatusLine().getStatusCode();
                if (status >= 200 && status < 300) {
                    HttpEntity entity = response.getEntity();
                    return entity != null ? Base64.decodeBase64(EntityUtils.toString(entity)) : null;
                } else {
                    throw new ClientProtocolException("Unexpected response status: " + status);
                }
            };

            return (AdminReply) new AdminReply().readFromBytes(httpclient.execute(httpPost, responseHandler));
        } catch (IOException ex) {
            Logger.getLogger(APIClient.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
}
