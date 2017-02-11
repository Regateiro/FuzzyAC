/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.messaging.rabbitmq;

/**
 * Public list of queues to use by the clients.
 * @author Diogo Regateiro
 */
public abstract class RMQPublicConstants {
    /**
     * The queue that is used to handle user queries.
     * Client applications should use this queue.
     */
    public static final String QUEUE_QUERY_REQUEST = ":query_request";
    public static final String QUEUE_QUERY_RESPONSE = ":query_response";
}
