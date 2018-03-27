/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.dfcl2.test;

import it.av.fac.dfcl2.DFIS;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.antlr.runtime.RecognitionException;

/**
 *
 * @author Diogo Regateiro <diogoregateiro@ua.pt>
 */
public class DFCLTest {

    public static void main(String[] args) throws IOException, RecognitionException {
        Map<String, Double> input = new HashMap<>();
        input.put("Revision", 0.0);

        String dfcl = fileToString(new File(System.getProperty("user.home") + "\\Documents\\NetBeansProjects\\FuzzyAC\\java\\FuzzyAC\\FAC_Module_Decision\\ores.dfcl"));

        for (int i = 0; i < 1000000; i++) {
            DFIS dfis = new DFIS(dfcl, true);
            ORESService ores = new ORESService("Revision");
            dfis.registerExternalInputFuzzifierService(ores);
            dfis.evaluate(input, false);
        }

        //calculateAccuracy(dfis, ores);
    }

    private static String fileToString(File file) {
        StringBuilder strb = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = in.readLine()) != null) {
                strb.append(line).append("\n");
            }
        } catch (IOException ex) {
            Logger.getLogger(DFCLTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        return strb.toString().trim();
    }

    private static void calculateAccuracy(DFIS dfis, ORESService ores) {
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
