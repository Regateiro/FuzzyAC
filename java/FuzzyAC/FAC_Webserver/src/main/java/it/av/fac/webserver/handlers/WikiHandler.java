/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.webserver.handlers;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import it.av.fac.enforcement.handlers.BDFISConnector;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Diogo Regateiro
 */
public class WikiHandler {

    private static BDFISConnector connector;
    private static WikiPageFetcher fetcher;

    public WikiHandler() {
        try {
            connector = BDFISConnector.getInstance();
            fetcher = WikiPageFetcher.getInstance("127.0.0.1", 27017);
        } catch (Exception ex) {
            Logger.getLogger(WikiHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public String fetch(String userToken, String pageName) throws IOException {
        StringBuilder html = new StringBuilder("<html>").append("<body>");

        JSONArray pages = fetcher.fetchPage(pageName.toLowerCase());
        if (pages.isEmpty()) {
            html.append("<h1>Page Not Found!</h1><h2>Similar pages:</h2>");
            pages = fetcher.search(pageName.toLowerCase());
            for (int i = 0; i < pages.size(); i++) {
                String page = pages.getJSONObject(i).getString("title");
                html.append("[[").append(page).append("]]</br>");
            }
        } else {
            JSONObject filteredPage = connector.filterPage(pages.getJSONObject(0), userToken, "read", true);
            //append wrapper div
            html.append("<style type=\"text/css\">").append(".wrapit {word-wrap: break-word;}").append("</style>");
            html.append("<div class=\"wrapit\">\n");
            JSONArray sections = JSONArray.parseArray(filteredPage.getString("text"));

            for (int i = 0; i < sections.size(); i++) {
                JSONObject section = sections.getJSONObject(i);
                html.append(String.format("<h%d>%s</h%d>\n", section.getIntValue("level"), section.getString("heading"), section.getIntValue("level")));
                JSONArray paragraphs = section.getJSONArray("paragraphs");
                for (int j = 0; j < paragraphs.size(); j++) {
                    html.append(String.format("<p>%s</p>\n", paragraphs.getString(j)));
                }
            }

            html.append("</div>");
        }

        return html.append("</body></html>").toString();
    }

    /**
     * TODO: SIMPLIFY THE ERROR MESSAGE!
     *
     * @param ex
     * @return
     */
    public String generateErrorPage(Exception ex) {
        StringBuilder html = new StringBuilder("<html>").append("<body>");
        html.append("<h1>Sorry, an error occurred...</h1><h2>");
        html.append("Details:</h2><p>");

        html.append("<b>").append(ex).append("</b></br>");
        for (StackTraceElement e : ex.getStackTrace()) {
            html.append(e.toString()).append("</br>");
        }

        return html.append("</p></body>").append("</html>").toString();
    }

    public String store(JSONObject resource, String userToken) {
        StringBuilder html = new StringBuilder("<html>").append("<body>");
        
        resource.put("_id", resource.getString("_id").toLowerCase());
        resource.put("title", resource.getString("title").toLowerCase());

        if (connector.store(resource, userToken)) {
            html.append("<h1>")
                    .append("The resource was stored successfully!")
                    .append("</h1>");
        } else {
            html.append("<h1>")
                    .append("The resource was NOT stored.")
                    .append("</h1>");
        }

        return html.append("</body>").append("</html>").toString();
    }
}
