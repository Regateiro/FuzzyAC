/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.messaging.rabbitmq;

/**
 * Complete list of queues to use by the architecture.
 * @author Diogo Regateiro
 */
public abstract class RabbitMQConstants {
    
    public static final String EXCHANGE = "FAC";
    
    /**
     * The queue that is used to handle user queries.
     * Client applications should use this queue.
     */
    public static final String QUEUE_QUERY_REQUEST = ":query_request";
    public static final String QUEUE_QUERY_RESPONSE = ":query_response";
    
    /**
     * The queue that is used to handle administration requests.
     */
    public static final String QUEUE_ADMIN = "Admin";
    
    /**
     * Queues for RIaC.
     */
    public static final String QUEUE_RIAC_REQUEST = ":riac_request";
    public static final String QUEUE_RIAC_RESPONSE = ":riac_response";
    
    /**
     * Queues for DBI.
     */
    public static final String QUEUE_DBI_REQUEST = ":dbi_request";
    public static final String QUEUE_DBI_RESPONSE = ":dbi_response";
    
    /**
     * Queues for Decision.
     */
    public static final String QUEUE_DECISION_REQUEST = ":enforcement_request";
    public static final String QUEUE_DECISION_RESPONSE = ":enforcement_response";
    /**
     * Queues for Information.
     */
    public static final String QUEUE_INFORMATION_REQUEST = ":information_request";
    public static final String QUEUE_INFORMATION_RESPONSE = ":information_response";
}
