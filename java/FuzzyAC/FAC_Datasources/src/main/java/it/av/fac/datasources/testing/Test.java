/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.datasources.testing;

import it.av.fac.datasources.wikipedia.WikipediaSource;
import it.av.fac.driver.APIClient;
import it.av.fac.driver.messages.QueryRequest;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/**
 *
 * @author DiogoJosé
 */
public class Test {
    public static void main(String[] args) {
        try {
            //APIClient fac = new APIClient("http://localhost:8084/FAC_Webserver");
            //System.out.println(fac.queryRequest(new QueryRequest()));
            //System.exit(0);
            
            WikipediaSource src = new WikipediaSource("E:\\articles.xml");
            src.parse();
        } catch (ParserConfigurationException | SAXException ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
