package it.av.wikipedia.client;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import it.av.wikipedia.client.parameters.FormatValue;
import it.av.wikipedia.client.parameters.ListValue;
import it.av.wikipedia.client.parameters.PropValue;
import it.av.wikipedia.client.parameters.QueryParameters;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

/**
 * 
 * @author Diogo Regateiro
 */
public class WikipediaClient {

    private final static String ENDPOINT = "https://en.wikipedia.org/w/api.php";
    
    public static String Query(QueryParameters params) {
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            StringBuilder url = new StringBuilder(ENDPOINT);
            url.append(params.toURLQueryString());
            
            System.out.println("GET: " + url);
            HttpGet request = new HttpGet(url.toString());
            request.setHeader("User-Agent", "FACWikiTool");

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

            return httpclient.execute(request, responseHandler);
        } catch (IOException ex) {
            Logger.getLogger(WikipediaClient.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
    
    public static void main(String[] args) throws IOException {
        QueryParameters params = new QueryParameters();
        params.format(FormatValue.json, true);
        params.list(ListValue.allusers);
        params.rawcontinue(true);
        params.redirects(true);
        params.subparameter("auactiveusers", "true");
        params.subparameter("auwitheditsonly", "true");
        params.subparameter("aulimit", "50"); // can request 500 at a time
        params.subparameter("auprop", "editcount");
        System.out.println(new JSONObject(WikipediaClient.Query(params)).toString(2));
        
        System.in.read();
        
        params.clear();
        params.format(FormatValue.json, true);
        params.list(ListValue.usercontribs);
        params.rawcontinue(true);
        params.redirects(true);
        params.subparameter("uclimit", "500");
        params.subparameter("ucprop", "ids|title|timestamp|sizediff|oresscores");
        params.subparameter("ucuserids", "3491742"); // can request 50 at a time
        System.out.println(new JSONObject(WikipediaClient.Query(params)).toString(2));
    }
}
