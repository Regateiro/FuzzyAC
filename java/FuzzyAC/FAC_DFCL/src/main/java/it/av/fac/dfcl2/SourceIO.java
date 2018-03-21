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
public class SourceIO {
    private final String name;
    private final List<String> inputs;

    public SourceIO(String sourceName) {
        this.name = sourceName;
        this.inputs = new ArrayList<>();
    }

    public String getName() {
        return name;
    }
    
    public void addInput(String inputName) {
        this.inputs.add(inputName);
    }
    
    public void addOutput(String outputName) {
        this.inputs.add(outputName);
    }
    
    public List<String> getInputs() {
        return Collections.unmodifiableList(inputs);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + Objects.hashCode(this.name);
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
        
        final SourceIO other = (SourceIO) obj;
        return this.name.equals(other.name);
    }
}
