/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.datasources.wikipedia;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;
import java.util.TreeSet;
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
public class WikipediaIndexer {

    private final SAXParser parser;
    private final String XMLFilePath;
    private final Set<String> titles;

    public WikipediaIndexer(String XMLFilePath) throws ParserConfigurationException, SAXException {
        this.parser = SAXParserFactory.newInstance().newSAXParser();
        this.titles = new TreeSet<>();
        this.XMLFilePath = XMLFilePath;
    }

    public void parse() {
        try (PrintWriter pw = new PrintWriter("F:\\wiki_titles.txt")) {
            this.parser.parse(new File(XMLFilePath), new PageHandler((Page page) -> {
                titles.add(page.getTitle());
            }));
            
            titles.stream().forEachOrdered((title) -> {
                pw.println(title);
            });

            //this.parser.parse(new File(XMLFilePath), new CategoryTaxonomyHandler("E:\\taxonomy.txt"));
        } catch (SAXException | IOException ex) {
            Logger.getLogger(WikipediaIndexer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void main(String[] args) throws ParserConfigurationException, SAXException {
        WikipediaIndexer src = new WikipediaIndexer("F:\\articles.xml");
        src.parse();
    }
}
