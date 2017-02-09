/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.messaging.rabbitmq;

/**
 * Enumeration of Queues.
 * Do not reference this enum directly, use the (Public/Internal)Queues classes.
 * @author Diogo Regateiro
 */
public enum Queue {
    QUERY("Query"), 
    ADMIN("Admin");

    private Queue(String queue_id) {
        this.queue_id = queue_id;
    }

    @Override
    public String toString() {
        return this.queue_id;
    }
    
    private final String queue_id;
}
