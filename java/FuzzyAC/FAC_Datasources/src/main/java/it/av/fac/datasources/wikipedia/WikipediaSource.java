/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.datasources.wikipedia;

import it.av.fac.driver.APIClient;
import it.av.fac.messaging.client.StorageReply;
import it.av.fac.messaging.client.StorageRequest;
import it.av.fac.messaging.client.StorageRequest.StorageRequestType;
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
    private final APIClient fac;

    public WikipediaSource(String XMLFilePath) throws ParserConfigurationException, SAXException {
        this.parser = SAXParserFactory.newInstance().newSAXParser();
        this.XMLFilePath = XMLFilePath;
        this.fac = new APIClient("http://localhost:8080/FAC_Webserver");
    }

    public void parse() {
        try {
            this.parser.parse(new File(XMLFilePath), new PageHandler((Page page) -> {
                if (!page.getTitle().startsWith("File:") && !page.getTitle().startsWith("Wikipedia:") 
                        && !page.getTitle().startsWith("Category:") && !page.getTitle().startsWith("Template:")) {
                    if ((int) (Math.random() * 10000) == 0) {
                        StorageRequest request = new StorageRequest();
                        request.setRequestType(StorageRequestType.StoreDocument);
                        request.setDocument(page.getText());
                        request.setStorageId("randwikipages");
                        request.setAditionalInfo("title", page.getTitle());
                        request.setAditionalInfo("redirecting", String.valueOf(page.isRedirecting()));
                        System.out.print("Storing: " + page.getTitle() + " ... ");
                        StorageReply reply = fac.storageRequest(request);
                        System.out.println("[" + reply.getStatus().name() + "] " + reply.getErrorMsg());
                    }
                }
            }));

            //this.parser.parse(new File(XMLFilePath), new CategoryTaxonomyHandler("E:\\taxonomy.txt"));
        } catch (SAXException | IOException ex) {
            Logger.getLogger(WikipediaSource.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void main(String[] args) throws ParserConfigurationException, SAXException {
        WikipediaSource src = new WikipediaSource("G:\\articles.xml");
        src.parse();
    }
}
