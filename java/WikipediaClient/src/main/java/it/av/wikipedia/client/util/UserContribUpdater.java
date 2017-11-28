/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.wikipedia.client.util;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author DiogoJosé
 */
public class UserContribUpdater extends TimerTask {

    private final WikipediaUtil WikipediaAPI;
    private final IStorage<JSONObject> userStorage;
    private final IStorage<JSONObject> contribStorage;
    private final DateFormat df;

    public UserContribUpdater(IStorage<JSONObject> userStorage, IStorage<JSONObject> contribStorage) {
        this.WikipediaAPI = new WikipediaUtil();
        this.userStorage = userStorage;
        this.contribStorage = contribStorage;
        this.df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    }

    @Override
    public void run() {
        try {
            //updateUsers();
            updateContributions(false);
            //updateUserStatistics();
        } catch (IOException ex) {
            Logger.getLogger(UserContribUpdater.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            userStorage.close();
            contribStorage.close();
        }
    }

    private void updateUsers() {
        JSONArray array = WikipediaAPI.GetAllUsers(true, true, null);
        do {
            for (int i = 0; i < array.length(); i++) {
                JSONObject user = array.getJSONObject(i);
                if (!userStorage.insert(user)) {
                    System.out.println("Unable to store: " + user);
                }
            }
        } while (!((array = WikipediaAPI.next()).length() == 0));
    }

    private void updateContributions(boolean restart) throws IOException {
        List<JSONObject> users = this.userStorage.select(new JSONObject());

        for (JSONObject user : users) {
            JSONObject id = new JSONObject();
            id.put("userid", user.get("userid"));
            System.out.println(id);

            String continueCode = (restart ? null : user.optString("continueCode"));
            if (continueCode != null && continueCode.equalsIgnoreCase(":Completed:")) {
                System.out.println(" - Previously completed. Skipping...");
                continue;
            }

            System.out.println(" - Processing new contributions...");
            JSONArray batchContribs = WikipediaAPI.GetAllUserContributions(user.get("userid").toString(), continueCode, "20171101000000");

            do {
                System.out.println(" ---> Retrieved " + batchContribs.length() + " new contributions.");
                System.out.println(" ---> Storing contributions...");
                for (int i = 0; i < batchContribs.length(); i++) {
                    try {
                        JSONObject contrib = batchContribs.getJSONObject(i);
                        JSONObject revInfo = new JSONObject();
                        revInfo.put("userid", contrib.getInt("userid"));
                        revInfo.put("pagetitle", contrib.getString("title"));
                        revInfo.put("timestamp", df.parse(contrib.getString("timestamp"))); // check only a few years?
                        revInfo.put("oresscores", contrib.getJSONObject("oresscores"));
                        contribStorage.insert(revInfo);
                    } catch (ParseException ex) {
                        Logger.getLogger(UserContribUpdater.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

                JSONObject update = new JSONObject();
                continueCode = WikipediaAPI.GetContinueCode();
                update.put("continueCode", (continueCode != null ? continueCode : ":Completed:"));
                System.out.println(" ---> Setting continue code " + (continueCode != null ? continueCode : ":Completed:") + "...");
                this.userStorage.update(id, update);
            } while (!((batchContribs = WikipediaAPI.next()).length() == 0));
            System.out.println();
        }
    }

    private void updateUserStatistics() {

    }

    public static void main(String[] args) throws IOException {
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new UserContribUpdater(
                new MongoDBStorage("127.0.0.1", 27017, "fac", "wikiusers"),
                new MongoDBStorage("127.0.0.1", 27017, "fac", "contribs")
        ), 0, 1000 * 60 * 60 * 24);
        System.out.println("DBI Server is now running... enter 'q' to quit.");
        while (System.in.read() != 'q');
        timer.cancel();
    }
}
