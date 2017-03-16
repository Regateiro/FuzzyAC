/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.webserver.handlers;

import it.av.fac.messaging.client.QueryReply;
import it.av.fac.messaging.client.QueryRequest;
import it.av.fac.messaging.client.ReplyStatus;
import it.av.fac.messaging.interfaces.IClientHandler;
import it.av.fac.messaging.rabbitmq.RabbitMQClient;
import it.av.fac.messaging.rabbitmq.RabbitMQConnectionWrapper;
import it.av.fac.messaging.rabbitmq.RabbitMQConstants;
import it.av.fac.webserver.WebserverConfig;
import java.io.IOException;
import java.util.concurrent.SynchronousQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class responsible for handling RequestAPI requests.
 *
 * @author Diogo Regateiro
 */
public class QueryHandler implements Handler<QueryRequest, QueryReply> {

    private static QueryHandler instance;
    private final SynchronousQueue<QueryReply> queue = new SynchronousQueue<>();
    private final RabbitMQClient conn;

    private QueryHandler() throws Exception {
        this.conn = new RabbitMQClient(RabbitMQConnectionWrapper.getInstance(),
                RabbitMQConstants.QUEUE_QUERY_RESPONSE,
                RabbitMQConstants.QUEUE_QUERY_REQUEST, WebserverConfig.MODULE_KEY, handler);
    }

    public static QueryHandler getInstance() throws Exception {
        if (instance == null) {
            instance = new QueryHandler();
        }

        return instance;
    }

    @Override
    public QueryReply handle(QueryRequest request) {
        QueryReply reply = new QueryReply();

        try {
            conn.send(request.convertToBytes());
            reply = queue.take();
        } catch (IOException | InterruptedException ex) {
            reply.setStatus(ReplyStatus.ERROR);
            reply.setErrorMsg(ex.getMessage());
        }

        return reply;
    }

    private final IClientHandler<byte[]> handler = (final byte[] replyBytes) -> {
        QueryReply reply = new QueryReply();
        try {
            reply.readFromBytes(replyBytes);
        } catch (IOException ex) {
            reply.setStatus(ReplyStatus.ERROR);
            reply.setErrorMsg(ex.getMessage());
        }

        try {
            queue.put(reply);
        } catch (InterruptedException ex) {
            Logger.getLogger(StorageHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    };

}
