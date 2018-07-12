/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.messaging.rabbitmq;

/**
 * Complete list of queues to use by the architecture.
 * @author Diogo Regateiro <diogoregateiro@ua.pt>
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
    public static final String QUEUE_DECISION_REQUEST = ":decision_request";
    public static final String QUEUE_DECISION_RESPONSE = ":decision_response";
    
    /**
     * Queues for Information.
     */
    public static final String QUEUE_INFORMATION_REQUEST = ":information_request";
    public static final String QUEUE_INFORMATION_RESPONSE = ":information_response";
    
    /**
     * Queues for Policy Retrieval.
     */
    public static final String QUEUE_POLICY_RETRIEVAL_REQUEST = ":policyretrieval_request";
    public static final String QUEUE_POLICY_RETRIEVAL_RESPONSE = ":policyretrieval_response";
    
     /**
     * Queues for Monitor.
     */
    public static final String QUEUE_MONITOR_REQUEST = ":monitor_request";
    public static final String QUEUE_MONITOR_RESPONSE = ":monitor_response";
}
