/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.datasources.wikipedia;

import it.av.fac.driver.APIClient;
import it.av.fac.driver.messages.ResourceReply;
import it.av.fac.driver.messages.ResourceRequest;
import it.av.fac.driver.messages.ResourceRequest.ResourceRequestType;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.SAXException;

/**
 * Splits a XML file into manageable chunks.
 *
 * @author Diogo Regateiro
 */
public class WikipediaSource {

    private final SAXParser parser;
    private final String XMLFilePath;

    public WikipediaSource(String XMLFilePath) throws ParserConfigurationException, SAXException {
        this.parser = SAXParserFactory.newInstance().newSAXParser();
        this.XMLFilePath = XMLFilePath;
    }

    public void parse() {
        try {
            this.parser.parse(new File(XMLFilePath), new PageHandler((Page page) -> {
                APIClient fac = new APIClient("http://localhost:8084/FAC_Webserver");
                ResourceRequest request = new ResourceRequest(ResourceRequestType.ADD_DOCUMENT);
                request.setPayload(page);
                ResourceReply reply = fac.resourceRequest(request);
            }));
            
            //this.parser.parse(new File(XMLFilePath), new CategoryTaxonomyHandler("E:\\taxonomy.txt"));
        } catch (SAXException | IOException ex) {
            Logger.getLogger(WikipediaSource.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void main(String[] args) throws ParserConfigurationException, SAXException {
        WikipediaSource src = new WikipediaSource("E:\\articles.xml");
        src.parse();
    }
}
