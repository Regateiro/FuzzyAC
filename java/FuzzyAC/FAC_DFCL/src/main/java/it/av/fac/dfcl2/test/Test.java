/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.dfcl2.test;

import it.av.fac.dfcl2.DFIS;
import it.av.fac.dfcl2.EIFS;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.antlr.runtime.RecognitionException;

/**
 *
 * @author Diogo Regateiro <diogoregateiro@ua.pt>
 */
public class Test {

    public static void main(String[] args) throws IOException, RecognitionException {
        DFIS dfis = new DFIS(new File("C:\\Users\\DiogoJosé\\Documents\\NetBeansProjects\\FuzzyAC\\java\\FuzzyAC\\FAC_Module_Decision\\ores.dfcl"), true);
        ORESService ores = new ORESService("Revision");
        dfis.registerExternalInputFuzzifierService(ores);

        Map<String, Double> input = new HashMap<>();

        int correct = 0;
        for (int revision = 0; revision < ores.getDataSize(); revision++) {
            input.put("Revision", (double) revision);
            boolean allow = dfis.evaluate(input, false).get("OutEdit").getValue() > 0.5;
            boolean allowExpected = ores.getExpectedResult(revision) > 0.5;
            if (allow == allowExpected) {
                correct++;
            }
        }
        
        System.out.println(String.format("%d/%d correct: %03f%%", correct, ores.getDataSize(), correct * 100.0 / ores.getDataSize()));
    }
}
