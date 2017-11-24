/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.wikipedia.client.util;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author DiogoJosé
 */
public class UserContribUpdater extends TimerTask {

    private final WikipediaUtil WikipediaAPI;
    private final IStorage<JSONObject> storage;
    private final DateFormat df;

    public UserContribUpdater(IStorage<JSONObject> storage) {
        this.WikipediaAPI = new WikipediaUtil();
        this.storage = storage;
        this.df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    }

    @Override
    public void run() {
        //updateUsers();
        updateContributions();
        //updateUserStatistics();
    }

    private void updateUsers() {
        JSONArray array = WikipediaAPI.GetAllUsers(true, true);
        do {
            for (int i = 0; i < array.length(); i++) {
                JSONObject user = array.getJSONObject(i);
                if (!storage.insert(user)) {
                    System.out.println("Unable to store: " + user);
                }
            }
        } while (!((array = WikipediaAPI.next()).length() == 0));
    }

    private void updateContributions() {
        List<JSONObject> users = this.storage.select(new JSONObject("{ contribs: { $exists: false } }"));

        for (JSONObject user : users) {
            JSONObject id = new JSONObject();
            id.put("userid", user.get("userid"));

            System.out.print(id + "\r");

            JSONArray batchContribs = WikipediaAPI.GetAllUserContributions(user.get("userid").toString());
            JSONArray userContribs = new JSONArray();

            do {
                System.out.print(String.format("%s %d%%\r", id.toString(), (userContribs.length() * 100) / user.getInt("editcount")));
                batchContribs.forEach((uCont) -> {
                    userContribs.put(uCont);
                });
            } while (!((batchContribs = WikipediaAPI.next()).length() == 0));

            JSONObject update = new JSONObject();
            update.put("contribs", userContribs);

            if (!this.storage.update(id, update)) {
                System.out.println("Unable to update: " + id);
            } else {
                System.out.println("Updated: " + id);
            }
        }
    }

    private void updateUserStatistics() {

    }

    public static void main(String[] args) throws IOException {
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new UserContribUpdater(new MongoDBStorage("127.0.0.1", 27017, "fac", "wikiusers")), 0, 1000 * 60 * 60 * 24);
        System.out.println("DBI Server is now running... enter 'q' to quit.");
        while (System.in.read() != 'q');
        timer.cancel();
    }
}
