/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.datasources.testing;

import it.av.fac.datasources.wikipedia.XMLSplitter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author DiogoJosé
 */
public class Test {
    public static void main(String[] args) {
        try(XMLSplitter xmls = new XMLSplitter("C:\\Users\\Public\\wikipedia\\abstracts.xml", "doc")) {
            Iterator<String> itr = xmls.iterator();
            
            int pages = 0;
            while(itr.hasNext()) {
                pages++;
            }
            
            System.out.println(pages);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
