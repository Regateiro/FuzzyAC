/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.driver.messages;

import it.av.fac.driver.messages.interfaces.IReply;

/**
 *
 * @author Diogo Regateiro
 */
public class QueryReply implements IReply {

    public QueryReply(String reply) {
        System.out.println(reply);
    }
    
}
