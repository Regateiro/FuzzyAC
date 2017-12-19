/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.datasources.wikipedia;

/**
 *
 * @author Diogo Regateiro <diogoregateiro@ua.pt>
 */
public interface Processor<T> {
    public void process(T a);
}
