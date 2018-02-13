/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.wikipedia.client.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Regateiro
 */
public class TrainingDatasetCreator {

    private static final String DATASET_PATH = new StringBuilder()
            .append(System.getProperty("user.home")).append(File.separator)
            .append("Google Drive").append(File.separator)
            .append("PhD").append(File.separator)
            .append("Thesis").append(File.separator)
            .append("Data").append(File.separator)
            .append("complete_dataset.gz").toString();

    public static void main(String[] args) {
        File dataset = new File(DATASET_PATH);
        File preparedDataset = new File("prepDataset.csv");
        assertRequirements(dataset);

        boolean verbose = false;
        int count = 0;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(dataset))));
                PrintWriter out = new PrintWriter(new FileWriter(preparedDataset, false))) {
            String line;
            while ((line = in.readLine()) != null) {
                JSONObject user = new JSONObject(line);
                if (verbose) {
                    System.out.println("Processing revisions by user [" + user.getString("name") + "]...");
                }

                JSONArray revisions = user.getJSONArray("revisions");
                for (int revIdx = 0; revIdx < revisions.length(); revIdx++) {
                    JSONObject revision = revisions.getJSONObject(revIdx);
                    if (verbose) {
                        System.out.println(" ~~~ [" + revision.getInt("revid") + "]: " + revision.optString("comment", "{No Comment}"));
                    }

                    boolean someoneElseRevised = false;
                    boolean wasUndone = false;
                    boolean hadRollbacks = false;
                    List<Integer> revertableRevsWithoutUndoing = new ArrayList<>();
                    revertableRevsWithoutUndoing.add(revision.getInt("revid"));

                    JSONArray nextRevisions = revision.getJSONArray("next_revisions");
                    for (int nextRevIdx = 0; nextRevIdx < nextRevisions.length(); nextRevIdx++) {
                        JSONObject nextRevision = nextRevisions.getJSONObject(nextRevIdx);
                        revertableRevsWithoutUndoing.add(nextRevision.getInt("revid"));

                        if (nextRevision.optInt("userid", -1) != user.getInt("userid")) {
                            someoneElseRevised = true;
                        }

                        String nextComment = nextRevision.optString("comment", "{No Comment}").toLowerCase();

                        if (nextComment.contains("reverted to revision")) {
                            if (verbose) {
                                System.out.println("  `--> " + nextComment);
                            }

                            boolean revertUndidUserRev = true;
                            for (int revid : revertableRevsWithoutUndoing) {
                                if (nextComment.contains(String.valueOf(revid))) {
                                    revertUndidUserRev = false;
                                }
                            }

                            if (revertUndidUserRev) {
                                wasUndone = true;
                                count++;
                            }
                        } else if ((nextComment.contains("reverted") || nextComment.contains("reverting")) && nextComment.contains(user.optString("name", "?user?with?no?name?").toLowerCase())) {
                            if (verbose) {
                                System.out.println("  `--> " + nextComment);
                            }
                            wasUndone = true;
                            count++;
                        } else if (nextComment.contains("undid revision") && nextComment.contains(String.valueOf(revision.getInt("revid")))) {
                            if (verbose) {
                                System.out.println("  `--> " + nextComment);
                            }
                            wasUndone = true;
                            count++;
                        } else if (nextComment.contains("rollback")) {
                            if (verbose) {
                                System.out.println("  `--> " + nextComment);
                            }
                            hadRollbacks = true;
                        }
                    }

                    if (!hadRollbacks && someoneElseRevised) {
                        out.println(toCSV(revision, wasUndone));
                        out.flush();
                    }
                }

                if (verbose) {
                    System.out.println();
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(TrainingDatasetCreator.class.getName()).log(Level.SEVERE, null, ex);
        }

        System.out.println("Found a total of " + count + " undone revisions.");
    }

    private static void assertRequirements(File dataset) {
        if (!dataset.exists()) {
            System.err.println("Error! Dataset file does not exist!");
            System.exit(1);
        } else if (!dataset.isFile()) {
            System.err.println("Error! Dataset path does not point to a file!");
            System.exit(2);
        } else if (!dataset.canRead()) {
            System.err.println("Error! Dataset file cannot be read!");
            System.exit(3);
        }
    }

    private static String toCSV(JSONObject revision, boolean wasUndone) {
        JSONObject scores = revision.getJSONObject("oresscores");
        JSONObject draftQuality = scores.getJSONObject("draftquality");
        JSONObject damaging = scores.getJSONObject("damaging");
        JSONObject goodFaith = scores.getJSONObject("goodfaith");
        
        return String.format("%f,%f,%f,%f,%f,%f,%f,%f,%d", 
                draftQuality.getDouble("OK"),
                draftQuality.getDouble("attack"),
                draftQuality.getDouble("spam"),
                draftQuality.getDouble("vandalism"),
                damaging.getDouble("true"),
                damaging.getDouble("false"),
                goodFaith.getDouble("true"),
                goodFaith.getDouble("false"),
                (wasUndone ? 0 : 1)
        );
    }
}
