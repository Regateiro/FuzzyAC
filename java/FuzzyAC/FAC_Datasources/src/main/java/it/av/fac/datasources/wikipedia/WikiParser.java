/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.datasources.wikipedia;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author DiogoJosé
 */
public class WikiParser {

    private static JSONArray processSections(String title, String text) {
        JSONArray sections = new JSONArray();
        Pattern pattern = Pattern.compile("^([=]+)([^=]+)[=]+$");
        JSONObject currentSection = createSection(title, 1);
        sections.put(currentSection);

        try (BufferedReader dumpReader = new BufferedReader(new StringReader(text))) {
            String line;
            while ((line = dumpReader.readLine()) != null) {
                int i;
                if (!line.equalsIgnoreCase("") && !line.equalsIgnoreCase("*")) {
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        i = matcher.group(1).length();
                        String subSectionTitle = matcher.group(2);
                        currentSection = createSection(subSectionTitle, i);
                        sections.put(currentSection);
                    } else {
                        currentSection.getJSONArray("paragraphs").put(line);
                    }
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(WikiParser.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return sections;
    }

    private static JSONObject createSection(String title, int level) {
        JSONObject section = new JSONObject();
        section.put("heading", title);
        section.put("level", level);
        section.put("paragraphs", new JSONArray());
        return section;
    }

    public static JSONArray parseText(Page page) {
        String pageText = page.getText();
        
        int length;
        do {
            length = pageText.length();
            pageText = pageText.replaceAll("<[^>]*\\/>", ""); //selfclosed tags and comment blocks
        } while (pageText.length() != length);

        do {
            length = pageText.length();
            pageText = pageText.replaceAll("<[^>]+>[^<>]*<\\/[ a-z0-9]+>", "");
        } while (pageText.length() != length);

        Pattern pattern = Pattern.compile("\\{\\{[^{}]*\\}\\}", Pattern.DOTALL);
        do {
            length = pageText.length();
            pageText = pattern.matcher(pageText).replaceAll("");
        } while (pageText.length() != length);

        pattern = Pattern.compile("<!--.*?-->", Pattern.DOTALL);
        do {
            length = pageText.length();
            pageText = pattern.matcher(pageText).replaceAll("");
        } while (pageText.length() != length);

        pattern = Pattern.compile("(\\[\\[[^\\]]*)(\\[\\[.*?\\]\\])([^\\[]*\\]\\])");
        do {
            length = pageText.length();
            pageText = pattern.matcher(pageText).replaceAll("$1$3");
        } while (pageText.length() != length);

        pattern = Pattern.compile("\\[\\[(Image:|Category:|File:|Draft:|Portal:|Template:)[^\\[\\]]+\\]\\]");
        do {
            length = pageText.length();
            pageText = pattern.matcher(pageText).replaceAll("");
        } while (pageText.length() != length);

        pageText = pageText.replaceAll("[ ]+", " ");
        pageText = pageText.replaceAll("\n+", "\n");
        pageText = pageText.trim();

        return processSections(page.getTitle(), pageText);
    }
}
