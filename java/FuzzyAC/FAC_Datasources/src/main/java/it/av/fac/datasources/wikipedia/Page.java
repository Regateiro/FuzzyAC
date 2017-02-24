/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.datasources.wikipedia;

/**
 *
 * @author Diogo Regateiro
 */
public class Page {

    private boolean redirecting;
    private String title;
    private int id;
    private String text;

    void setRedirecting(boolean redirecting) {
        this.redirecting = redirecting;
    }

    boolean isRedirecting() {
        return this.redirecting;
    }

    void setTitle(String title) {
        this.title = title;
    }

    void setId(int id) {
        this.id = id;
    }

    void setText(String text) {
        this.text = text;
    }

    public String getTitle() {
        return title;
    }

    public int getId() {
        return id;
    }

    public String getText() {
        return text;
    }
}
