/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.dbi.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author DiogoJosé
 */
public class WikiParser {

    public static String clean(String title, String pageDump) {
        pageDump = removeTags(pageDump, "<ref", "<ref", "\\/\\s*(ref)?\\s*>");
        pageDump = removeTags(pageDump, "{{", "{{", "\\}\\}");
        pageDump = removeTags(pageDump, "[[Image:", "[[", "\\]\\]");
        pageDump = removeTags(pageDump, "[[Category:", "[[", "\\]\\]");
        pageDump = removeTags(pageDump, "[[File:", "[[", "\\]\\]");
        pageDump = removeTags(pageDump, "<!--", "<!--", "\\-\\-\\>");
        pageDump = pageDump.replaceAll("\\=\\=\\=\\=\\=\\=([^=]*)\\=\\=\\=\\=\\=\\=", "<h6>$1</h3>");
        pageDump = pageDump.replaceAll("\\=\\=\\=\\=\\=([^=]*)\\=\\=\\=\\=\\=", "<h5>$1</h3>");
        pageDump = pageDump.replaceAll("\\=\\=\\=\\=([^=]*)\\=\\=\\=\\=", "<h4>$1</h3>");
        pageDump = pageDump.replaceAll("\\=\\=\\=([^=]*)\\=\\=\\=", "<h3>$1</h3>");
        pageDump = pageDump.replaceAll("\\=\\=([^=]*)\\=\\=", "<h2>$1</h2>");
        pageDump = String.format("<h1>%s</h1>\n\n%s", title, pageDump.trim());
        return pageDump;
    }
    
    private static String removeTags(String pageDump, String rootMatch, String openTag, String closeTagRegex) {
        Pattern p = Pattern.compile(closeTagRegex);

        while (true) {
            int rootOpenTagIdx = pageDump.indexOf(rootMatch);

            if (rootOpenTagIdx == -1) {
                //No more tags in the text.
                break;
            }

            int currentIdx = rootOpenTagIdx;
            int openenedTags = 1;
            int closeTagSize = 1;
            while (openenedTags != 0) {
                int nextOpenTagIdx = pageDump.indexOf(openTag, currentIdx + openTag.length());
                Matcher m = p.matcher(pageDump.substring(currentIdx + closeTagSize));
                int nextCloseTagIdx = -1;
                
                if(m.find()) {
                    nextCloseTagIdx = m.start() + currentIdx + closeTagSize;
                    closeTagSize = m.end() - m.start();
                }

                //If not all tags have been closed and no close tag exists, then a mismatched tag was found... nothing can be done.
                if (nextCloseTagIdx == -1) {
                    System.out.println("Unclosed " + openTag + " tag.");
                    break;
                }

                //If the next open tag does not exist or the next open tag does not occur before a close tag appears
                if (nextOpenTagIdx == -1 || nextCloseTagIdx < nextOpenTagIdx) {
                    //decrement the counter as a tag as been closed.
                    openenedTags--;
                    currentIdx = nextCloseTagIdx;

                    if (openenedTags == 0) {
                        //All tags have been properly closed. Remove everything from the root to the last close tag.
                        pageDump = pageDump.substring(0, rootOpenTagIdx) + pageDump.substring(nextCloseTagIdx + closeTagSize);
                    }
                } else {
                    // There is another tag being opened, increase the counter and set the current opened tag to the next one
                    openenedTags++;
                    currentIdx = nextOpenTagIdx;
                }
            }
        }
        return pageDump;
    }
}
