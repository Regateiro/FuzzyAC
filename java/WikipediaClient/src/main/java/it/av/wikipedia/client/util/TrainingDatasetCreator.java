/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.wikipedia.client.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author Diogo Regateiro <diogoregateiro@ua.pt>
 */
public class TrainingDatasetCreator {

    private static final WikipediaUtil WIKIPEDIA = new WikipediaUtil();
    private static final int NUM_CONTRIBS = 10;
    private static final int NUM_AFTER_REVS = 5;

    public static void main(String[] args) {

        File usersFile = new File("wikiusers.txt");
        File contribsFile = new File("wikicontribs.txt");
        File pagerevsFolder = new File("pagerevs");

        // Create the users file if it does not exist
        if (!usersFile.exists()) {
            System.out.println("Users file not found. Creating...");
            createUsersFile(usersFile);
        }

        // Create the contribs file if it does not exist
        checkContribsFile(usersFile, contribsFile);

        checkPageRevisions(contribsFile, pagerevsFolder);
    }

    public static void createUsersFile(File usersFile) {
        try (PrintWriter out = new PrintWriter(usersFile)) {
            System.out.println(" - Getting all users...");
            JSONArray juserarr = WIKIPEDIA.GetAllUsers(true, true, null);
            do {
                for (int i = 0; i < juserarr.length(); i++) {
                    out.println(juserarr.getJSONObject(i));
                }
            } while (!((juserarr = WIKIPEDIA.next()).length() == 0));
        } catch (FileNotFoundException ex) {
            Logger.getLogger(TrainingDatasetCreator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void checkContribsFile(File usersFile, File contribsFile) {
        int lastUserId = -1;

        // find which user was processed last if the file exists
        if (contribsFile.exists()) {
            try (BufferedReader in_contribs = new BufferedReader(new FileReader(contribsFile))) {
                String line;
                while ((line = in_contribs.readLine()) != null) {
                    JSONObject contrib = new JSONObject(line);
                    lastUserId = contrib.getInt("userid");
                }
            } catch (IOException ex) {
                Logger.getLogger(TrainingDatasetCreator.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        // For each user
        try (BufferedReader in_users = new BufferedReader(new FileReader(usersFile));
                PrintWriter out = new PrintWriter(new FileWriter(contribsFile, true))) {
            String line;
            while ((line = in_users.readLine()) != null) {
                JSONObject user = new JSONObject(line);

                // Skip user if already processed
                if (lastUserId != -1) {
                    if (user.getInt("userid") == lastUserId) {
                        lastUserId = -1;
                    }
                    continue;
                }

                // Get the most recent contributions
                System.out.println(" - Getting recent contributions from user [" + user.getString("name") + "]...");
                JSONArray contribs = getUserRecentContributions(String.valueOf(user.getInt("userid")));
                for (int i = 0; i < contribs.length(); i++) {
                    out.println(contribs.getJSONObject(i));
                }
                out.flush();
            }
        } catch (IOException ex) {
            Logger.getLogger(TrainingDatasetCreator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void checkPageRevisions(File contribsFile, File pagerevsFolder) {
        if (!pagerevsFolder.exists()) {
            pagerevsFolder.mkdir();
        }

        // For each revision
        try (BufferedReader in_revs = new BufferedReader(new FileReader(contribsFile))) {
            String line;
            while ((line = in_revs.readLine()) != null) {
                JSONObject rev = new JSONObject(line);

                // Get the most recent contributions
                System.out.println(" - Getting next contributions on the page for revision [" + rev.getInt("revid") + "]...");
                JSONArray contribs = getPageContributionsAfterRevision(String.valueOf(rev.getInt("pageid")), String.valueOf(rev.getInt("revid")));

                try (PrintWriter out = new PrintWriter(new FileWriter(new File(pagerevsFolder, String.valueOf(rev.getInt("revid"))), true))) {
                    for (int i = 0; i < contribs.length(); i++) {
                        out.println(contribs.getJSONObject(i));
                    }
                    out.flush();
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(TrainingDatasetCreator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static JSONArray getUserRecentContributions(String userid) {
        JSONArray ret = new JSONArray();

        JSONArray jcontribarr = WIKIPEDIA.GetRecentUserContributions(userid, NUM_CONTRIBS, null);
        do {
            for (int i = 0; i < jcontribarr.length(); i++) {
                ret.put(jcontribarr.getJSONObject(i));
            }
        } while (ret.length() < NUM_CONTRIBS && !((jcontribarr = WIKIPEDIA.next()).length() == 0));

        while (ret.length() > NUM_CONTRIBS) {
            ret.remove(NUM_CONTRIBS);
        }

        return ret;
    }

    private static JSONArray getPageContributionsAfterRevision(String pageid, String revid) {
        JSONArray ret = new JSONArray();

        JSONArray jcontribarr = WIKIPEDIA.GetPageContributionsAfterRevision(pageid, revid, NUM_AFTER_REVS + 1, null);
        do {
            for (int i = 0; i < jcontribarr.length(); i++) {
                ret.put(jcontribarr.getJSONObject(i));
            }
        } while (ret.length() < NUM_AFTER_REVS && !((jcontribarr = WIKIPEDIA.next()).length() == 0));

        while (ret.length() > NUM_AFTER_REVS) {
            ret.remove(NUM_AFTER_REVS);
        }

        return ret;
    }
}
