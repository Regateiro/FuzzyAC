/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.webserver.handlers;

import com.alibaba.fastjson.JSONObject;
import it.av.fac.enforcement.handlers.BDFISConnector;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Diogo Regateiro
 */
public class WikiHandler {

    private static BDFISConnector connector;

    public WikiHandler() {
        try {
            connector = BDFISConnector.getInstance();
        } catch (Exception ex) {
            Logger.getLogger(WikiHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public String fetch(String userToken, String page) {
        StringBuilder html = new StringBuilder("<html>").append("<body>");
        if (connector.canAccess(page, userToken, "read", true)) {
            html.append("<h1>").append("YEY").append("</h1>");
        } else {
            html.append("<h1>").append("NAY").append("</h1>");
        }

        return html.append("</body>").append("</html>").toString();
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
        for(StackTraceElement e : ex.getStackTrace()) {
            html.append(e.toString()).append("</br>");
        }
        
        return html.append("</p></body>").append("</html>").toString();
    }

    public String store(JSONObject resource, String userToken) {
        StringBuilder html = new StringBuilder("<html>").append("<body>");

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
