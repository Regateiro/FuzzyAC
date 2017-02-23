/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.datasources.wikipedia;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Splits a XML file into manageable chunks.
 *
 * @author Diogo Regateiro
 */
public class XMLSplitter implements Closeable, Iterable<String> {

    private final BufferedReader in;
    private final String itrTag;
    private final String itrTagEnd;

    public XMLSplitter(String XMLFilePath, String itrTag) throws FileNotFoundException {
        this.in = new BufferedReader(new FileReader(XMLFilePath));
        this.itrTag = String.format("<%s>", itrTag);
        this.itrTagEnd = String.format("</%s>", itrTag);
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

    @Override
    public Iterator<String> iterator() {
        return new Iterator<String>() {
            private boolean checked = false;
            private boolean hasNext = hasNext();

            @Override
            public boolean hasNext() {
                // Check only if it hasn't checked this iteration
                if (!checked) {
                    try {
                        // assume there is no next value
                        this.hasNext = false;

                        // search the input file for the iteration tag
                        String line;
                        while ((line = in.readLine()) != null) {
                            // if the tag if found, set the hasNext variable to true
                            if (line.equalsIgnoreCase(itrTag)) {
                                this.hasNext = true;
                                break;
                            }
                        }

                        // set the checked status to true for this iteration
                        this.checked = true;
                    } catch (IOException ex) {
                    }
                }

                // return the stored value
                return this.hasNext;
            }

            @Override
            public String next() {
                // Get next only if one exists
                if (hasNext) {
                    try {
                        // Since we're reading the next item, set the next checked to false.
                        this.checked = false;

                        // Initialize the return with the iteration tag
                        StringBuilder ret = new StringBuilder(itrTag).append(System.lineSeparator());

                        // Add lines that do not match the end iteration tag to the return
                        String line;
                        while ((line = in.readLine()) != null && !line.equalsIgnoreCase(itrTagEnd)) {
                            ret.append(line).append(System.lineSeparator());
                        }

                        // Since we skipped it, add the end iteration tag to the return
                        ret.append(itrTagEnd);

                        // Reinitialize the hasNext and checked variables.
                        hasNext();

                        // return
                        return ret.toString();
                    } catch (IOException ex) {
                        Logger.getLogger(XMLSplitter.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

                return null;
            }
        };
    }
}
