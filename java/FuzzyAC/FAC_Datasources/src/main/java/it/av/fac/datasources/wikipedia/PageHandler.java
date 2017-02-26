/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.datasources.wikipedia;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author Diogo Regateiro
 */
public class PageHandler extends DefaultHandler {

    private Page page;
    private StringBuilder stringBuilder;
    private boolean idSet = false;
    private final Processor<Page> pageProcessor;

    public PageHandler(Processor<Page> pageProcessor) {
        super();
        this.pageProcessor = pageProcessor;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

        stringBuilder = new StringBuilder();

        if (qName.equals("page")) {

            page = new Page();
            idSet = false;

        } else if (qName.equals("redirect")) {
            if (page != null) {
                page.setRedirecting(true);
            }
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {

        if (page != null && !page.isRedirecting()) {
            switch (qName) {
                case "title":
                    page.setTitle(stringBuilder.toString());
                    break;
                case "id":
                    if (!idSet) {
                        page.setId(Integer.parseInt(stringBuilder.toString()));
                        idSet = true;
                    }
                    break;
                case "text":
                    String articleText = stringBuilder.toString();
                    articleText = articleText.replaceAll("(?s)<ref(.+?)</ref>", " "); //remove references
                    articleText = articleText.replaceAll("(?s)\\{\\{(.+?)\\}\\}", " "); //remove links underneath headings
                    articleText = articleText.replaceAll("(?s)==See also==.+", " "); //remove everything after see also
                    articleText = articleText.replaceAll("\\|", " "); //Separate multiple links
                    articleText = articleText.replaceAll("[\\t\\s]*[\\n][\\t\\n\\s]*", "\n"); //remove new lines
                    articleText = articleText.replaceAll("[^a-zA-Z0-9- \\s]", " "); //remove all non alphanumeric except dashes and spaces
                    articleText = articleText.trim().replaceAll(" +", " "); //convert all multiple spaces to 1 space
                    page.setText(articleText);
                    break;
                case "page":
                    pageProcessor.process(page);
                    page = null;
                    break;
                default:
                    break;
            }
        } else {
            page = null;
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        stringBuilder.append(ch, start, length);
    }
}