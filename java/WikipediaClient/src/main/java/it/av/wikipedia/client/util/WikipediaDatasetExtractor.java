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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author Diogo Regateiro <diogoregateiro@ua.pt>
 */
public class WikipediaDatasetExtractor {

    private static final WikipediaUtil WIKIPEDIA = new WikipediaUtil();
    private static final int NUM_REVISIONS = 10;
    private static final int NUM_AFTER_REVS = 5;

    public static void main(String[] args) {

        File dataset = new File("dataset.txt");

        // Create the users file if it does not exist
//        if (!usersFile.exists()) {
//            System.out.println("Users file not found. Creating...");
//            createUsersFile(usersFile);
//        }
        // Create the contribs file if it does not exist
        //checkContribsFile(usersFile, contribsFile);
        checkPageRevisions(dataset);
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
            Logger.getLogger(WikipediaDatasetExtractor.class.getName()).log(Level.SEVERE, null, ex);
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
                Logger.getLogger(WikipediaDatasetExtractor.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        // For each user
        try (BufferedReader in_users = new BufferedReader(new FileReader(usersFile))) {
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

                try (PrintWriter out = new PrintWriter(new FileWriter(contribsFile, true))) {
                    for (int i = 0; i < contribs.length(); i++) {
                        out.println(contribs.getJSONObject(i));
                    }
                    out.flush();
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(WikipediaDatasetExtractor.class.getName()).log(Level.SEVERE, null, ex);
        }

        //correct contribs file
        fixContributions(contribsFile);
    }

    private static void checkPageRevisions(File dataset) {
        File completeDataset = new File("complete_" + dataset.getName());

        int continueFromUserId = -1;
        try (BufferedReader in = new BufferedReader(new FileReader(completeDataset))) {
            String line;
            while ((line = in.readLine()) != null) {
                JSONObject user = new JSONObject(line);
                continueFromUserId = user.getInt("userid");
            }
        } catch (IOException ex) {
        }

        boolean foundLastProcessedUser = (continueFromUserId == -1);
        // For each revision
        try (BufferedReader in = new BufferedReader(new FileReader(dataset))) {
            String line;
            while ((line = in.readLine()) != null) {
                JSONObject user = new JSONObject(line);
                if (!foundLastProcessedUser) {
                    System.out.println(" - Skipping already processed user [" + user.getString("name") + "]...");
                    if (user.getInt("userid") == continueFromUserId) {
                        foundLastProcessedUser = true;
                    }
                    continue;
                }

                JSONArray revisions = user.getJSONArray("revisions");
                for (int i = 0; i < revisions.length(); i++) {
                    JSONObject revision = revisions.getJSONObject(i);

                    // Get the following revisions on the page
                    System.out.println(" - Getting next revisions on the page for user [" + user.getString("name") + "] and revision [" + revision.getInt("revid") + "]...");
                    JSONArray nextRevisions = getPageContributionsAfterRevision(String.valueOf(revision.getInt("pageid")), String.valueOf(revision.getInt("revid")));
                    revision.put("next_revisions", nextRevisions);
                }

                try (PrintWriter out = new PrintWriter(new FileWriter(completeDataset, true))) {
                    out.println(user);
                    out.flush();
                }

            }
        } catch (IOException ex) {
            Logger.getLogger(WikipediaDatasetExtractor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static JSONArray getUserRecentContributions(String userid) {
        JSONArray ret = new JSONArray();

        JSONArray jcontribarr = WIKIPEDIA.GetUserContributions(userid, NUM_REVISIONS, null, false);
        do {
            for (int i = 0; i < jcontribarr.length(); i++) {
                JSONObject contrib = jcontribarr.getJSONObject(i);
                if (contrib.keySet().contains("oresscores") && contrib.getJSONObject("oresscores").keySet().contains("draftquality")) {
                    ret.put(contrib);
                }
            }
        } while (ret.length() < NUM_REVISIONS && !((jcontribarr = WIKIPEDIA.next()).length() == 0));

        while (ret.length() > NUM_REVISIONS) {
            ret.remove(NUM_REVISIONS);
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
        } while (ret.length() < NUM_AFTER_REVS + 1 && !((jcontribarr = WIKIPEDIA.next()).length() == 0));

        ret.remove(0); //remove the original revision from the user

        while (ret.length() > NUM_AFTER_REVS) {
            ret.remove(NUM_AFTER_REVS);
        }

        return ret;
    }

    private static void fixContributions(File contribsFile) {
        File userrevs = new File("userrevs.txt");
        int lastUserId = -1;

        try (BufferedReader in_revs = new BufferedReader(new FileReader(userrevs))) {
            String line;
            String lastRevision = null;
            while ((line = in_revs.readLine()) != null) {
                lastRevision = line;
            }
            if (lastRevision != null) {
                lastUserId = new JSONObject(lastRevision).getInt("userid");
            }
        } catch (IOException ex) {
            Logger.getLogger(WikipediaDatasetExtractor.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (lastUserId != -1) {
            System.out.println("Resuming from userid [" + lastUserId + "]...");
        }

        boolean canContinue = false;
        try (BufferedReader in_revs = new BufferedReader(new FileReader(contribsFile))) {
            List<JSONObject> revBuffer = new ArrayList<>();

            boolean hasMoreRevs = true;
            while (hasMoreRevs) {
                String line;
                // fill the buffer with revisions
                while (revBuffer.size() < NUM_REVISIONS) {
                    if ((line = in_revs.readLine()) != null) {
                        JSONObject rev = new JSONObject(line);
                        int userid = rev.getInt("userid");
                        if (!canContinue) {
                            if (lastUserId == -1 || userid == lastUserId) {
                                canContinue = true;
                            }
                        }
                        if (canContinue && userid != lastUserId) {
                            revBuffer.add(rev);
                        }
                    } else {
                        hasMoreRevs = false;
                        break;
                    }
                }

                if (revBuffer.isEmpty()) {
                    break;
                }

                // set the current user being processed
                int currentUserId = revBuffer.get(0).getInt("userid");

                // request more revisions for a user if all NUM_REVISIONS revisions from it were previously collected but at least one is missing the draftquality
                boolean requestMoreRevisions = revBuffer.stream().allMatch((revision) -> {
                    return revision.getInt("userid") == currentUserId;
                }) && revBuffer.stream().anyMatch((revision) -> {
                    return !revision.keySet().contains("oresscores") || !revision.getJSONObject("oresscores").keySet().contains("draftquality");
                });

                // If new revisions are required
                if (requestMoreRevisions) {
                    // Get the most recent contributions
                    System.out.println(" - Getting recent contributions from user [" + currentUserId + "]...");
                    JSONArray contribs = getUserRecentContributions(String.valueOf(currentUserId));

                    try (PrintWriter out = new PrintWriter(new FileWriter(userrevs, true))) {
                        for (int i = 0; i < contribs.length(); i++) {
                            out.println(contribs.getJSONObject(i));
                        }
                        out.flush();
                    }
                } else {
                    // If the number of revisions is less than 10 or all of them have the required fields, then requesting more reviews is unecessary
                    // Save all the existing reviews with the fields necessary from the buffer that match the current user
                    System.out.println(" - Filtering and saving existing revisions from user [" + currentUserId + "]...");
                    try (PrintWriter out = new PrintWriter(new FileWriter(userrevs, true))) {
                        revBuffer.stream()
                                .filter((revision) -> revision.getInt("userid") == currentUserId)
                                .filter((revision) -> revision.keySet().contains("oresscores"))
                                .filter((revision) -> revision.getJSONObject("oresscores").keySet().contains("draftquality"))
                                .forEach((revision) -> out.println(revision));
                        out.flush();
                    }
                }

                // remove the revisions from the current user from the buffer
                while (!revBuffer.isEmpty() && revBuffer.get(0).getInt("userid") == currentUserId) {
                    revBuffer.remove(0);
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(WikipediaDatasetExtractor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
