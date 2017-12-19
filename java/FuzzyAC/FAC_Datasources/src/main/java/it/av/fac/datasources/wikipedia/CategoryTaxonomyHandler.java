/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.datasources.wikipedia;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author Diogo Regateiro <diogoregateiro@ua.pt>
 */
public class CategoryTaxonomyHandler extends DefaultHandler {

    private StringBuilder stringBuilder;
    private boolean isCategory = false;
    private Category cat;
    private final PrintWriter out;

    public CategoryTaxonomyHandler(String outputPath) throws FileNotFoundException {
        this.out = new PrintWriter(new File(outputPath));
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        stringBuilder = new StringBuilder();
        if (qName.equalsIgnoreCase("page")) {
            if (isCategory) {
                out.println(cat);
                out.flush();
            }
            isCategory = false;
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (qName.equals("title")) {
            String title = stringBuilder.toString();
            if (title.startsWith("Category:")) {
                isCategory = true;
                cat = new Category();
                cat.setName(title.substring((title.length() <= 9 ? 0 : 9)));
            }
        } else if (isCategory && qName.equalsIgnoreCase("text")) {
            String[] parents = stringBuilder.toString().split("\\r?\\n");
            for (String parent : parents) {
                if (parent.startsWith("[[Category:")) {
                    try {
                        cat.addParentCategory(parent.substring(11, parent.length() - 2));
                    } catch (StringIndexOutOfBoundsException ex) {
                    }
                }
            }
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        stringBuilder.append(ch, start, length);
    }

    @Override
    public void endDocument() throws SAXException {
        out.flush();
        out.close();
    }
}
