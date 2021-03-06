/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.webserver.handlers;

import it.av.fac.enforcement.handlers.BDFISConnector;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author Diogo Regateiro <diogoregateiro@ua.pt>
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

        connector.getLogger().info("Fetching page " + pageName.toLowerCase());
        JSONArray pages = fetcher.fetchPage(pageName.toLowerCase());
        if (pages.length() == 0) {
            html.append("<h1>Page Not Found!</h1><h2>Similar pages:</h2>");

            connector.getLogger().info("Page not found. Searching for similar pages.");
            pages = fetcher.search(pageName.toLowerCase());
            for (int i = 0; i < pages.length(); i++) {
                String page = pages.getJSONObject(i).getString("title");
                html.append("[[").append(page).append("]]</br>");
            }
        } else {
            connector.getLogger().info("Constructing html page.");
            //JSONObject filteredPage = connector.filterPage(pages.getJSONObject(0), userToken, "read");
            JSONObject filteredPage = connector.flagWritableSections(pages.getJSONObject(0), userToken);

            //append wrapper div
            html.append("<style type=\"text/css\">").append(".wrapit {word-wrap: break-word;}").append("</style>");
            html.append("<div class=\"wrapit\">\n");
            JSONArray sections = filteredPage.getJSONArray("text");

            for (int i = 0; i < sections.length(); i++) {
                JSONObject section = sections.getJSONObject(i);
                html.append(String.format("<h%d>%s</h%d>\n", section.getInt("level"), section.getString("heading") + (section.getBoolean("editable") ? " (Edit)" : ""), section.getInt("level")));
                JSONArray paragraphs = section.getJSONArray("paragraphs");
                for (int j = 0; j < paragraphs.length(); j++) {
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
        connector.getLogger().error(ex.getMessage());
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
            connector.getLogger().info("Stored data: " + resource.toString());
            html.append("<h1>")
                    .append("The resource was stored successfully!")
                    .append("</h1>");
        } else {
            connector.getLogger().error("Could not store data: " + resource.toString());
            html.append("<h1>")
                    .append("The resource was NOT stored.")
                    .append("</h1>");
        }

        return html.append("</body>").append("</html>").toString();
    }

    public String registerUser(String userName) {
        StringBuilder html = new StringBuilder("<html>").append("<body>");

        String userData;
        if ((userData = connector.registerUser(userName)) != null) {
            JSONObject jUserData = new JSONObject(userData);
            
            connector.getLogger().info("Registered user " + userName + " with data " + userData);
            html.append("<h1>")
                    .append("The user was registered successfully!")
                    .append("</h1><p>User info: ").append(userData).append("</p>")
                    .append("<p><a href=\"http://localhost:8080/FAC_Webserver/WikiAPI/Portugal?token=")
                    .append(jUserData.getString("token"))
                    .append("\">Start using the system!</a></p>");
        } else {
            connector.getLogger().error("Could not register user " + userName);
            html.append("<h1>")
                    .append("The user was NOT registered.")
                    .append("</h1>");
        }

        return html.append("</body>").append("</html>").toString();
    }

    public String getUser(String userToken) {
        StringBuilder html = new StringBuilder("<html>").append("<body>");

        JSONObject userInfo;
        if ((userInfo = connector.getUserInfo(userToken)) != null) {
            connector.getLogger().info("Retrieved user " + userInfo.toString());
            html.append("<pre>")
                    .append(userInfo.toString(2))
                    .append("</pre>");
        } else {
            connector.getLogger().warning("Could not retrieve user (does it exist?) with token " + userToken);
            html.append("<h1>")
                    .append("The user was NOT found.")
                    .append("</h1>");
        }

        return html.append("</body>").append("</html>").toString();
    }
}
