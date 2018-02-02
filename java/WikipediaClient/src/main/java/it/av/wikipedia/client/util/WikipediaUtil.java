/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.wikipedia.client.util;

import it.av.wikipedia.client.WikipediaClient;
import it.av.wikipedia.client.parameters.FormatValue;
import it.av.wikipedia.client.parameters.ListValue;
import it.av.wikipedia.client.parameters.PropValue;
import it.av.wikipedia.client.parameters.QueryParameters;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author Regateiro
 */
public class WikipediaUtil {

    private QueryParameters params;
    private String continueLabel;
    private String[] path;
    private String queryParam;
    private boolean batchComplete;

    public WikipediaUtil() {
        batchComplete = true;
    }

    public boolean hasNext() {
        return !batchComplete;
    }

    public String GetContinueCode() {
        return params.getSubParameter(continueLabel);
    }

    public JSONArray next() {
        JSONArray ret = new JSONArray();

        if (hasNext()) {
            JSONObject reply;
            do {
                reply = new JSONObject(WikipediaClient.Query(params));
            } while (isNonQueryReply(reply));

            if (reply.keySet().contains("continue")) {
                params.subparameter(continueLabel, reply.getJSONObject("continue").getString(continueLabel));
            } else {
                params.subparameter(continueLabel, null);
                batchComplete = true;
            }

            reply = reply.getJSONObject("query");
            if(path != null) {
                for(String step : path) {
                    reply = reply.getJSONObject(step);
                }
            }
            
            ret = reply.getJSONArray(queryParam);
        }

        return ret;
    }

    public JSONArray GetAllUsers(boolean withRecentActivity, boolean withEditsOnly, String continueCode) {
        params = new QueryParameters();
        params.format(FormatValue.json, true);
        params.list(ListValue.allusers);
        if (withRecentActivity) {
            params.subparameter("auactiveusers", "true");
        }
        if (withEditsOnly) {
            params.subparameter("auwitheditsonly", "true");
        }
        params.subparameter("aulimit", "500"); // can request 500 at a time
        params.subparameter("auprop", "editcount");
        params.subparameter("maxlag", "5");
        path = null;
        queryParam = "allusers";
        continueLabel = "aufrom";

        if (continueCode != null && !continueCode.equalsIgnoreCase("")) {
            batchComplete = false;
            params.subparameter(continueLabel, continueCode);
        }

        JSONObject reply;
        do {
            reply = new JSONObject(WikipediaClient.Query(params));
        } while (isNonQueryReply(reply));

        if (reply.keySet().contains("continue")) {
            batchComplete = false;
            params.subparameter(continueLabel, reply.getJSONObject("continue").getString(continueLabel));
        } else {
            batchComplete = true;
        }

        return reply.getJSONObject("query").getJSONArray(queryParam);
    }

    public JSONArray GetUserByName(String userName) {
        params = new QueryParameters();
        params.format(FormatValue.json, true);
        params.list(ListValue.users);
        params.subparameter("ususers", userName);
        params.subparameter("maxlag", "5");
        path = null;
        queryParam = "users";

        JSONObject reply;
        do {
            reply = new JSONObject(WikipediaClient.Query(params));
        } while (isNonQueryReply(reply));

        return reply.getJSONObject("query").getJSONArray(queryParam);
    }
    
    public JSONArray GetAllUserContributions(String userid) {
        return GetAllUserContributions(userid, null, null);
    }

    public JSONArray GetAllUserContributions(String userid, String continueCode, String ucend) {
        params = new QueryParameters();
        params.format(FormatValue.json, true);
        params.list(ListValue.usercontribs);
        params.subparameter("uclimit", "500"); // can request 500 at a time
        params.subparameter("ucprop", "ids|title|timestamp|comment|size|sizediff|flags|tags|oresscores");
        params.subparameter("ucuserids", userid);
        params.subparameter("ucdir", "newer");
        if (ucend != null) {
            params.subparameter("ucend", ucend);
        }
        params.subparameter("maxlag", "5");
        path = null;
        queryParam = "usercontribs";
        continueLabel = "uccontinue";

        if (continueCode != null && !continueCode.equalsIgnoreCase("")) {
            batchComplete = false;
            params.subparameter(continueLabel, continueCode);
        }

        JSONObject reply;
        do {
            reply = new JSONObject(WikipediaClient.Query(params));
        } while (isNonQueryReply(reply));

        if (reply.keySet().contains("continue")) {
            batchComplete = false;
            params.subparameter(continueLabel, reply.getJSONObject("continue").getString(continueLabel));
        } else {
            batchComplete = true;
        }

        return reply.getJSONObject("query").getJSONArray(queryParam);
    }
    
    public JSONArray GetRecentUserContributions(String userid, int numContribs, String continueCode) {
        params = new QueryParameters();
        params.format(FormatValue.json, true);
        params.list(ListValue.usercontribs);
        params.subparameter("uclimit", String.valueOf(numContribs)); // can request 500 at a time
        params.subparameter("ucprop", "ids|title|timestamp|comment|size|sizediff|flags|tags|oresscores");
        params.subparameter("ucuserids", userid);
        params.subparameter("ucdir", "older");
        params.subparameter("maxlag", "5");
        path = null;
        queryParam = "usercontribs";
        continueLabel = "uccontinue";

        if (continueCode != null && !continueCode.equalsIgnoreCase("")) {
            batchComplete = false;
            params.subparameter(continueLabel, continueCode);
        }

        JSONObject reply;
        do {
            reply = new JSONObject(WikipediaClient.Query(params));
        } while (isNonQueryReply(reply));

        if (reply.keySet().contains("continue")) {
            batchComplete = false;
            params.subparameter(continueLabel, reply.getJSONObject("continue").getString(continueLabel));
        } else {
            batchComplete = true;
        }

        return reply.getJSONObject("query").getJSONArray(queryParam);
    }
    
    public JSONArray GetPageContributionsAfterRevision(String pageid, String revid, int numContribs, String continueCode) {
        params = new QueryParameters();
        params.format(FormatValue.json, true);
        params.prop(PropValue.revisions);
        params.subparameter("pageids", pageid);
        params.subparameter("rvlimit", String.valueOf(numContribs)); // can request 500 at a time
        params.subparameter("rvprop", "ids|timestamp|userid|comment|size|flags|flagged|tags|oresscores");
        params.subparameter("rvstartid", revid);
        params.subparameter("rvdir", "newer");
        params.subparameter("maxlag", "5");
        path = new String[] {"pages", pageid};
        queryParam = "revisions";
        continueLabel = "rvcontinue";

        if (continueCode != null && !continueCode.equalsIgnoreCase("")) {
            batchComplete = false;
            params.subparameter(continueLabel, continueCode);
        }

        JSONObject reply;
        do {
            reply = new JSONObject(WikipediaClient.Query(params));
        } while (isNonQueryReply(reply));

        if (reply.keySet().contains("continue")) {
            batchComplete = false;
            params.subparameter(continueLabel, reply.getJSONObject("continue").getString(continueLabel));
        } else {
            batchComplete = true;
        }

        return reply.getJSONObject("query").getJSONObject("pages").getJSONObject(pageid).getJSONArray(queryParam);
    }

    private boolean isNonQueryReply(JSONObject reply) {
        if (!reply.keySet().contains("query")) {
            try {
                System.out.print(reply.toString());
                Thread.sleep(5000);
            } catch (InterruptedException ex) {
                Logger.getLogger(WikipediaUtil.class.getName()).log(Level.SEVERE, null, ex);
            }
            return true;
        }

        return false;
    }
}
