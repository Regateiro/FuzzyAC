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
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.antlr.runtime.RecognitionException;

/**
 *
 * @author Diogo Regateiro <diogoregateiro@ua.pt>
 */
public class Test {

    public static void main(String[] args) throws IOException, RecognitionException {
        String fcle = fileToString(new File("C:\\Users\\Regateiro\\Documents\\NetBeansProjects\\FuzzyAC\\java\\FuzzyAC\\FAC_Module_Decision\\recommendation.fcle"));
        
        for (int i = 0; i < 1000000; i++) {
            DFIS dfis = new DFIS(fcle, true);
            ORESService ores = new ORESService("Revision");
            dfis.registerExternalSource(new RetailerSource("Retailer1", "Rec_by_R1"));
            dfis.registerExternalSource(new RetailerSource("Retailer2", "Rec_by_R2"));

            Map<String, Double> input = new HashMap<>();
            input.put("ProductID", 1.0);
            input.put("AvgCrossSale", 50.0);//Math.random() * 100);
            dfis.evaluate(input, false);
        }

//        int correct = 0;
//        for (int revision = 0; revision < ores.getDataSize(); revision++) {
//            input.put("Revision", (double) revision);
//            boolean allow = dfis.evaluate(input, false).get("OutEdit").getValue() > 0.5;
//            boolean allowExpected = ores.getExpectedResult(revision) > 0.5;
//            if (allow == allowExpected) {
//                correct++;
//            }
//        }
//        
//        System.out.println(String.format("%d/%d correct: %03f%%", correct, ores.getDataSize(), correct * 100.0 / ores.getDataSize()));
    }

    private static String fileToString(File file) {
        StringBuilder strb = new StringBuilder();
        try(BufferedReader in = new BufferedReader(new FileReader(file))) {
            String line;
            while((line = in.readLine()) != null) {
                strb.append(line).append("\n");
            }
        } catch (IOException ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        }
        return strb.toString().trim();
    }
}
