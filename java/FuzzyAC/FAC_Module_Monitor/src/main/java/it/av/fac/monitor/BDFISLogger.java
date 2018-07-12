/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.monitor;

import java.io.Closeable;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * @author Regateiro
 */
public class BDFISLogger implements Closeable {

    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyyMMdd");
    private static final SimpleDateFormat DETAILED_SDF = new SimpleDateFormat("HH:mm:ss.SSS");
    private final PrintWriter out;

    public BDFISLogger() throws IOException {
        this.out = new PrintWriter(new FileWriter(SDF.format(new Date() + ".log"), true), true);
    }
    
    public void log(LogLevel level, String module, String msg) {
        out.println(String.format("%s -> [%s] %s: %s", DETAILED_SDF.format(new Date()), level.name(), module, msg));
    }

    @Override
    public void close() throws IOException {
        this.out.flush();
        this.out.close();
    }
    
    public enum LogLevel {
        Info, Warning, Error;
    }
}
