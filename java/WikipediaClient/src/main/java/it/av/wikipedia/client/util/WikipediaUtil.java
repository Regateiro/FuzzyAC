/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.wikipedia.client.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONReader;
import com.alibaba.fastjson.JSONWriter;
import it.av.wikipedia.client.WikipediaClient;
import it.av.wikipedia.client.parameters.FormatValue;
import it.av.wikipedia.client.parameters.ListValue;
import it.av.wikipedia.client.parameters.QueryParameters;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Regateiro
 */
public class WikipediaUtil {

    private QueryParameters params;
    private String continueLabel;
    private String queryParam;
    private boolean batchComplete;

    public WikipediaUtil() {
        batchComplete = true;
    }

    public boolean hasNext() {
        return !batchComplete;
    }

    public JSONArray next() {
        if (hasNext()) {
            JSONObject reply = JSONObject.parseObject(WikipediaClient.Query(params));
            if (reply.containsKey("continue")) {
                params.subparameter(continueLabel, reply.getJSONObject("continue").getString(continueLabel));
            } else {
                batchComplete = true;
            }
            return reply.getJSONObject("query").getJSONArray(queryParam);
        }

        return new JSONArray();
    }

    public JSONArray GetAllUsers(boolean withRecentActivity, boolean withEditsOnly) {
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
        queryParam = "allusers";

        JSONObject reply = JSONObject.parseObject(WikipediaClient.Query(params));

        if (reply.containsKey("continue")) {
            batchComplete = false;
            continueLabel = "aufrom";
            params.subparameter(continueLabel, reply.getJSONObject("continue").getString(continueLabel));
        } else {
            batchComplete = true;
        }

        return reply.getJSONObject("query").getJSONArray(queryParam);
    }

    public JSONArray GetAllUserContributions(String userid) {
        params = new QueryParameters();
        params.format(FormatValue.json, true);
        params.list(ListValue.usercontribs);
        params.subparameter("uclimit", "500"); // can request 500 at a time
        params.subparameter("ucprop", "ids|title|timestamp|comment|size|sizediff|flags|tags|oresscores");
        params.subparameter("ucuserids", userid);
        queryParam = "usercontribs";

        JSONObject reply = JSONObject.parseObject(WikipediaClient.Query(params));

        if (reply.containsKey("continue")) {
            batchComplete = false;
            continueLabel = "uccontinue";
            params.subparameter(continueLabel, reply.getJSONObject("continue").getString(continueLabel));
        } else {
            batchComplete = true;
        }

        return reply.getJSONObject("query").getJSONArray(queryParam);
    }

    public static void main(String[] args) {
        WikipediaUtil util = new WikipediaUtil();

        try (JSONReader in = new JSONReader(new FileReader("allusers.json"))) {
            in.startArray();

            while (in.hasNext()) {
                JSONObject user = in.readObject(JSONObject.class);
                JSONArray usercontribs = util.GetAllUserContributions(user.getString("userid"));

                System.out.print("Processing user [" + user.getString("userid") + "]... ");
                try (JSONWriter out = new JSONWriter(new FileWriter(new File("usercontribs", user.getString("userid") + ".json")))) {
                    out.startArray();

                    do {
                        for (int i = 0; i < usercontribs.size(); i++) {
                            out.writeObject(usercontribs.getJSONObject(i));
                        }
                        out.flush();
                    } while (!(usercontribs = util.next()).isEmpty());

                    out.endArray();
                } catch (IOException ex) {
                    Logger.getLogger(WikipediaUtil.class.getName()).log(Level.SEVERE, null, ex);
                }
                System.out.println("Done!");
            }

            in.endArray();
        } catch (IOException ex) {
            Logger.getLogger(WikipediaUtil.class.getName()).log(Level.SEVERE, null, ex);
        }

//        JSONArray array = util.GetAllUsers(true, true);
//        
//        try (JSONWriter out = new JSONWriter(new FileWriter("allusers.json"))) {
//            out.startArray();
//
//            do {
//                for (int i = 0; i < array.size(); i++) {
//                    JSONObject user = array.getJSONObject(i);
//                    out.writeObject(user);
//                }
//                out.flush();
//            } while (!(array = util.next()).isEmpty());
//
//            out.endArray();
//        } catch (IOException ex) {
//            Logger.getLogger(WikipediaUtil.class.getName()).log(Level.SEVERE, null, ex);
//        }
    }
}
