/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.dfcl2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 *
 * @author Regateiro
 */
public class EnsembleBlock {
    public final List<String> sources;
    public String afisName;
    public String efisName;

    public EnsembleBlock(String afisName) {
        this.sources = new ArrayList<>();
        this.afisName = afisName;
        this.efisName = null;
    }

    public String getAFISName() {
        return afisName;
    }
    
    public String getEFISName() {
        return efisName;
    }

    public void setEFIS(String efisName) {
        this.efisName = efisName;
    }
    
    public void addSource(String source) {
        this.sources.add(source);
    }
    
    public List<String> getSources() {
        return Collections.unmodifiableList(sources);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 43 * hash + Objects.hashCode(this.efisName);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final EnsembleBlock other = (EnsembleBlock) obj;
        if (!Objects.equals(this.efisName, other.efisName)) {
            return false;
        }
        return true;
    }
}
