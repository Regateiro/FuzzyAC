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
public class FCLETest {

    public static void main(String[] args) throws IOException, RecognitionException {
        String fcle = fileToString(new File(System.getProperty("user.home") + "\\Documents\\NetBeansProjects\\FuzzyAC\\java\\FuzzyAC\\FAC_Module_Decision\\recommendation.fcle"));

        for (int i = 0; i < 10000; i++) {
            try {
                long time1 = System.nanoTime();
                DFIS dfis = new DFIS(fcle, true);
                time1 = System.nanoTime() - time1;
                
                ORESService ores = new ORESService("Revision");
                dfis.registerExternalSource(new RetailerSource("Retailer1", "Rec_by_R1"));
                dfis.registerExternalSource(new RetailerSource("Retailer2", "Rec_by_R2"));

                Map<String, Double> input = new HashMap<>();
                input.put("ProductID", 1.0);
                input.put("AvgCrossSale", Math.random() * 100);
                
                long time2 = System.nanoTime();
                dfis.evaluate(input, false);
                time2 = System.nanoTime() - time2;
                System.out.println((time1 + time2) / 1000000.0);
            } catch (NullPointerException ex) {
                i--;
            }
        }
    }

    private static String fileToString(File file) {
        StringBuilder strb = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = in.readLine()) != null) {
                strb.append(line).append("\n");
            }
        } catch (IOException ex) {
            Logger.getLogger(FCLETest.class.getName()).log(Level.SEVERE, null, ex);
        }
        return strb.toString().trim();
    }
}
