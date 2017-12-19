/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.riac.classifier;

import it.av.fac.messaging.client.interfaces.IRequest;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author Diogo Regateiro <diogoregateiro@ua.pt>
 */
public class TaxonomyClassifier implements IClassifier {

    private static final Map<String, Set<Object>> TAXONOMY = new HashMap<>();

    public TaxonomyClassifier() {
        if (TAXONOMY.isEmpty()) {
            reloadTaxonomy();
        }
    }

    public final void reloadTaxonomy() {
        System.out.print("Loading taxonomy... ");
        try (BufferedReader in = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream("taxonomy.gz"))))) {
            String line = null;
            while ((line = in.readLine()) != null) {
                String[] split = line.split("[:]", 2);
                String child = split[0];
                TAXONOMY.putIfAbsent(child, new HashSet<>());

                if (split.length == 2) {
                    List<String> parents = Arrays.asList(split[1].split("[|]"));
                    TAXONOMY.get(child).addAll(parents);
                }
            }
            System.out.println("OK!");
        } catch (IOException ex) {
            System.out.println("ERROR!");
            Logger.getLogger(TaxonomyClassifier.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void classify(IRequest request) {
        JSONObject resource = new JSONObject(request.getResource());
        JSONArray categories = new JSONArray((String) resource.get("categories"));
        Set<String> labels = new HashSet<>();

        search(new HashSet<>(categories.toList()), labels, 0);
        if (labels.isEmpty()) {
            labels.add("PUBLIC");
        } else if (labels.contains("ADMINISTRATIVE")) {
            labels.clear();
            labels.add("ADMINISTRATIVE");
        }

        StringBuilder labelstr = new StringBuilder();
        labels.forEach((label) -> {
            labelstr.append(label).append(";");
        });

        resource.put("security_labels", labelstr.toString());
        resource.put("sl_timestamp", String.valueOf(System.currentTimeMillis()));
        request.setResource(resource.toString());
    }

    private void search(Set<Object> categories, Set<String> labels, int depth) {
        if (depth < 5 && categories != null) {
            categories.parallelStream().forEach((category) -> {
                switch (((String) category).toLowerCase()) {
                    case "security":
                        syncAddToSet(labels, "ACADEMIC");
                        break;
                    case "economy":
                        syncAddToSet(labels, "BUSINESS");
                        break;
                    case "politics":
                        syncAddToSet(labels, "ADMINISTRATIVE");
                        break;
                    default:
                        search(TAXONOMY.get(category), labels, depth + 1);
                }
            });
        }
    }

    private synchronized void syncAddToSet(Set<String> set, String label) {
        set.add(label);
    }
}
