/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.wikipedia.client.util;

import java.util.List;

/**
 *
 * @author Diogo Regateiro <diogoregateiro@ua.pt>
 */
public interface IStorage<T> {
    public boolean insert(T data);
    public boolean update(T idField, T updatedFields);
    public boolean delete(T matchingData);
    public List<T> select(T matchingData);
    public void index(boolean unique, String ... keys);
    public void close();
}
