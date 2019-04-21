/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.datasources.wikipedia;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import it.av.fac.driver.APIClient;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.SAXException;

/**
 * Splits a XML file into manageable chunks.
 *
 * @author Diogo Regateiro <diogoregateiro@ua.pt>
 */
public class WikipediaSource {

    private final SAXParser parser;
    private final String XMLFilePath;
    private final APIClient fac;
    private long SKIP;

    public WikipediaSource(String XMLFilePath) throws ParserConfigurationException, SAXException {
        this.parser = SAXParserFactory.newInstance().newSAXParser();
        this.fac = new APIClient("http://localhost:8080/FAC_Webserver");
        this.XMLFilePath = XMLFilePath;

        try (MongoClient mongoClient = new MongoClient("127.0.0.1", 27017)) {
            MongoDatabase mongoDB = mongoClient.getDatabase("fac");
            MongoCollection metadataCol = mongoDB.getCollection("metadata");
            this.SKIP = metadataCol.count();
        }
    }

    public void parse() {
        try {
            System.out.println("Skipping " + SKIP + " documents...");
            
            InputStream XMLFileStream;
            if(this.XMLFilePath.endsWith("gz")) {
                XMLFileStream = new GZIPInputStream(new FileInputStream(new File(XMLFilePath)));
            } else if(this.XMLFilePath.endsWith(".xml")) {
                XMLFileStream = new FileInputStream(new File(XMLFilePath));
            } else {
                throw new IllegalArgumentException("File path must be a xml or a gzip compressed xml file.");
            }
            
            this.parser.parse(XMLFileStream, new PageHandler((Page page) -> {
                if (!page.getTitle().matches("^(File:|Wikipedia:|Category:|Draft:|Portal:|Template:).*$")) {
                    if (SKIP == 0) {
                        String parsedPage = WikiParser.parseText(page).toString();
                        page.setText(parsedPage);
                        fac.storageRequest("", page.toJSON());
                    } else {
                        SKIP--;
                    }
                }
            }));
        } catch (SAXException | IOException ex) {
            Logger.getLogger(WikipediaSource.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void main(String[] args) throws ParserConfigurationException, SAXException {
        WikipediaSource src = new WikipediaSource(args[0]);
        src.parse();
    }
}
