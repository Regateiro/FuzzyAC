/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.decision.handlers;

import it.av.fac.dfcl.DynamicFunction;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.JSONObject;

/**
 * Generates ORES related linguistic term membership degrees.
 *
 * @author Diogo Regateiro <diogoregateiro@ua.pt>
 */
public class ORES extends DynamicFunction {

    private static final String DRAFT_QUALITY = "Draft_Quality";
    private static final String DAMAGING = "Damaging";
    private static final String GOOD_FAITH = "Good_Faith";
    private final List<String> oresUserHistory;

    public ORES(List<String> oresUserHistory) {
        this.oresUserHistory = oresUserHistory;
    }

    @Override
    protected Map<String, Map<String, Double>> processInternal() {
        Map<String, Map<String, Double>> ret = new HashMap<>();
        ret.put(DRAFT_QUALITY, new HashMap<>());
        ret.put(DAMAGING, new HashMap<>());
        ret.put(GOOD_FAITH, new HashMap<>());

        ret.get(DRAFT_QUALITY).put("OK", 0.0);
        ret.get(DRAFT_QUALITY).put("Attack", 0.0);
        ret.get(DRAFT_QUALITY).put("Spam", 0.0);
        ret.get(DRAFT_QUALITY).put("Vandalism", 0.0);
        ret.get(DAMAGING).put("True", 0.0);
        ret.get(DAMAGING).put("False", 0.0);
        ret.get(GOOD_FAITH).put("True", 0.0);
        ret.get(GOOD_FAITH).put("False", 0.0);

        // use the average of the past month of oresscores
        final AtomicInteger counter = new AtomicInteger();
        oresUserHistory.stream()
                .map((scoresStr) -> new JSONObject(scoresStr))
                .filter((scores) -> scores.keySet().contains("oresscores"))
                .map((scores) -> scores.getJSONObject("oresscores"))
                .filter((oresscores) -> oresscores.keySet().containsAll(Arrays.asList("draftquality", "damaging", "goodfaith")))
                .forEach((oresscores) -> {
                    JSONObject draftquality = oresscores.getJSONObject("draftquality");
                    JSONObject damaging = oresscores.getJSONObject("damaging");
                    JSONObject goodfaith = oresscores.getJSONObject("goodfaith");

                    ret.get(DRAFT_QUALITY).put("OK", ret.get(DRAFT_QUALITY).get("OK") + draftquality.getDouble("OK"));
                    ret.get(DRAFT_QUALITY).put("Attack", ret.get(DRAFT_QUALITY).get("Attack") + draftquality.getDouble("attack"));
                    ret.get(DRAFT_QUALITY).put("Spam", ret.get(DRAFT_QUALITY).get("Spam") + draftquality.getDouble("spam"));
                    ret.get(DRAFT_QUALITY).put("Vandalism", ret.get(DRAFT_QUALITY).get("Vandalism") + draftquality.getDouble("vandalism"));
                    ret.get(DAMAGING).put("True", ret.get(DAMAGING).get("True") + damaging.getDouble("true"));
                    ret.get(DAMAGING).put("False", ret.get(DAMAGING).get("False") + damaging.getDouble("false"));
                    ret.get(GOOD_FAITH).put("True", ret.get(GOOD_FAITH).get("True") + goodfaith.getDouble("true"));
                    ret.get(GOOD_FAITH).put("False", ret.get(GOOD_FAITH).get("False") + goodfaith.getDouble("false"));

                    counter.incrementAndGet();
                });

        // calculate the averages
        ret.get(DRAFT_QUALITY).put("OK", ret.get(DRAFT_QUALITY).get("OK") / counter.get());
        ret.get(DRAFT_QUALITY).put("Attack", ret.get(DRAFT_QUALITY).get("Attack") / counter.get());
        ret.get(DRAFT_QUALITY).put("Spam", ret.get(DRAFT_QUALITY).get("Spam") / counter.get());
        ret.get(DRAFT_QUALITY).put("Vandalism", ret.get(DRAFT_QUALITY).get("Vandalism") / counter.get());
        ret.get(DAMAGING).put("True", ret.get(DAMAGING).get("True") / counter.get());
        ret.get(DAMAGING).put("False", ret.get(DAMAGING).get("False") / counter.get());
        ret.get(GOOD_FAITH).put("True", ret.get(GOOD_FAITH).get("True") / counter.get());
        ret.get(GOOD_FAITH).put("False", ret.get(GOOD_FAITH).get("False") / counter.get());

        return ret;
    }
}
