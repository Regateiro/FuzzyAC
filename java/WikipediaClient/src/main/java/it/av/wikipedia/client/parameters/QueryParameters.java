/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.wikipedia.client.parameters;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * https://en.wikipedia.org/w/api.php?action=help&modules=query
 * @author Diogo Regateiro <diogoregateiro@ua.pt>
 */
public class QueryParameters {

    /**
     * A map with all the parameters to use.
     */
    private final Map<String, String> params = new TreeMap<>();

    public void format(FormatValue value, boolean inUTF8) {
        params.put("format", value.name());
        if (inUTF8) {
            params.put("utf8", "");
        }
    }

    /**
     * Which properties to get for the queried pages.
     *
     * @param value
     */
    public void prop(PropValue value) {
        params.put("prop", value.name());
    }

    /**
     * Which lists to get.
     *
     * @param value
     */
    public void list(ListValue value) {
        params.put("list", value.name());
    }

    /**
     * Which metadata to get.
     *
     * @param value
     */
    public void meta(MetaValue value) {
        params.put("meta", value.name());
    }

    /**
     * Get the list of pages to work on by executing the specified query module.
     *
     * @param value
     */
    public void generator(GeneratorValue value) {
        params.put("generator", value.name());
    }

    /**
     * A list of titles to work on.
     *
     * @param titles
     */
    public void titles(String... titles) {
        StringBuilder titleList = new StringBuilder();
        if (titles.length > 0) {
            titleList.append(titles[0]);
            for (int i = 1; i < titles.length; i++) {
                titleList.append("|").append(titles[i]);
            }
            params.put("titles", titleList.toString());
        }
    }

    /**
     * Adds a subparameter to the query string. Useful for query continuation,
     * for example.
     *
     * @param subparam
     * @param value
     */
    public void subparameter(String subparam, String value) {
        params.put(subparam, value);
    }

    /**
     * Include an additional pageids section listing all returned page IDs.
     *
     * @param set
     */
    public void indexpageids(boolean set) {
        if (set) {
            params.put("indexpageids", "true");
        }
    }

    /**
     * Export the current revisions of all given or generated pages.
     *
     * @param set
     */
    public void export(boolean set) {
        if (set) {
            params.put("export", "true");
        }
    }

    /**
     * Return the export XML without wrapping it in an XML result (same format
     * as Special:Export). Can only be used with query+export.
     *
     * @param set
     */
    public void exportnowrap(boolean set) {
        if (set) {
            params.put("exportnowrap", "true");
        }
    }

    /**
     * Whether to get the full URL if the title is an interwiki link.
     *
     * @param set
     */
    public void iwurl(boolean set) {
        if (set) {
            params.put("iwurl", "true");
        }
    }

    /**
     * Return raw query-continue data for continuation.
     *
     * @param set
     */
    public void rawcontinue(boolean set) {
        if (set) {
            params.put("rawcontinue", "true");
        }
    }

    /**
     * Automatically resolve redirects in query+titles, query+pageids, and
     * query+revids, and in pages returned by query+generator.
     *
     * @param set
     */
    public void redirects(boolean set) {
        if (set) {
            params.put("redirects", "true");
        }
    }

    /**
     * Convert titles to other variants if necessary. Only works if the wiki's
     * content language supports variant conversion. Languages that support
     * variant conversion include en, gan, iu, kk, ku, shi, sr, tg, uz and zh.
     *
     * @param set
     */
    public void converttitles(boolean set) {
        if (set) {
            params.put("converttitles", "true");
        }
    }

    /**
     * A list of page IDs to work on.
     *
     * @param pageids
     */
    public void pageids(int... pageids) {
        StringBuilder pageidsList = new StringBuilder();
        if (pageids.length > 0) {
            pageidsList.append(pageids[0]);
            for (int i = 1; i < pageids.length; i++) {
                pageidsList.append("|").append(pageids[i]);
            }
            params.put("pageids", pageidsList.toString());
        }
    }

    /**
     * A list of revision IDs to work on.
     *
     * @param pageids
     */
    public void revids(int... pageids) {
        StringBuilder revidsList = new StringBuilder();
        if (pageids.length > 0) {
            revidsList.append(pageids[0]);
            for (int i = 1; i < pageids.length; i++) {
                revidsList.append("|").append(pageids[i]);
            }
            params.put("revids", revidsList.toString());
        }
    }

    /**
     * Creates the URL query string from the set parameters.
     *
     * @return
     */
    public String toURLQueryString() {
        return toString();
    }

    @Override
    public String toString() {
        StringBuilder urlQuery = new StringBuilder("?action=query");
        params.keySet().forEach((key) -> {
            try {
                urlQuery.append("&").append(key).append("=").append(URLEncoder.encode(params.get(key), "UTF-8"));
            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(QueryParameters.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        return urlQuery.toString();
    }

    public void clear() {
        this.params.clear();
    }

    public String getSubParameter(String subparam) {
        return params.get(subparam);
    }
}
