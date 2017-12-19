/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.datasources.wikipedia;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 
 * @author Diogo Regateiro <diogoregateiro@ua.pt>
 */
public class Category {
    private String name;
    private final List<String> parentCategories;

    public Category() {
        this.parentCategories = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public void addParentCategory(String subCategory) {
        this.parentCategories.add(subCategory);
    }
    
    public List<String> getParentCategories() {
        return Collections.unmodifiableList(parentCategories);
    }

    @Override
    public String toString() {
        StringBuilder strb = new StringBuilder(name).append(":");
        parentCategories.forEach((parent) -> {
            strb.append(parent).append("|");
        });
        strb.deleteCharAt(strb.length() - 1);
        return strb.toString();
    }
}
