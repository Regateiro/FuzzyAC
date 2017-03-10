/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.decision.handlers;

import it.av.fac.decision.util.DecisionConfig;
import it.av.fac.messaging.client.QueryReply;
import it.av.fac.messaging.client.QueryRequest;
import it.av.fac.messaging.client.ReplyStatus;
import it.av.fac.messaging.interfaces.IClientHandler;
import it.av.fac.messaging.interfaces.IFACConnection;
import it.av.fac.messaging.interfaces.IServerHandler;
import it.av.fac.messaging.rabbitmq.RabbitMQClient;
import it.av.fac.messaging.rabbitmq.RabbitMQConstants;
import it.av.fac.messaging.rabbitmq.RabbitMQConnectionWrapper;
import it.av.fac.messaging.rabbitmq.RabbitMQServer;
import it.av.fac.messaging.rabbitmq.test.Server;
import java.io.IOException;
import java.util.concurrent.SynchronousQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class responsible for handling query requests.
 *
 * @author Diogo Regateiro
 */
public class DecisionHandler implements IServerHandler<byte[], String> {

    private final SynchronousQueue<QueryReply> queue = new SynchronousQueue<>();
    private final RabbitMQClient conn;

    public DecisionHandler() throws Exception {
        this.conn = new RabbitMQClient(RabbitMQConnectionWrapper.getInstance(),
                RabbitMQConstants.QUEUE_INFORMATION_RESPONSE,
                RabbitMQConstants.QUEUE_INFORMATION_REQUEST, DecisionConfig.MODULE_KEY, handler);
    }

    @Override
    public void handle(byte[] requestBytes, String clientKey) {
        try (IFACConnection clientConn = new RabbitMQServer(
                RabbitMQConnectionWrapper.getInstance(),
                RabbitMQConstants.QUEUE_DECISION_RESPONSE, clientKey)) {
            QueryReply reply = handle(new QueryRequest().readFromBytes(requestBytes));
            System.out.println("Replying with " + reply.getStatus().name() + " : " + reply.getErrorMsg());
            clientConn.send(reply.convertToBytes());
        } catch (Exception ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private QueryReply handle(QueryRequest request) {
        QueryReply errorReply = new QueryReply();
        errorReply.setStatus(ReplyStatus.ERROR);

        System.out.println("Processing query: " + request.getQuery());
        System.out.println("Requesting documents...");
        System.out.println("Requesting access control decision for each document labels..."); //FAC_Module_Decision will verify the user attributes
        System.out.println("Filtering documents to which the user does not have read permission...");
        System.out.println("Replying with ");

        return errorReply;

    }
    
    /**
     * Classify and send the document to the DBI.
     *
     * @param request The request with the document to classify and store.
     * @return The storage process status.
     */
    private QueryReply requestDecision(QueryRequest request) {
        QueryReply errorReply = new QueryReply();

        try {
            conn.send(request.convertToBytes());
            return queue.take();
        } catch (IOException | InterruptedException ex) {
            errorReply.setStatus(ReplyStatus.ERROR);
            errorReply.setErrorMsg(ex.getMessage());
        }

        return errorReply;
    }
    
    private final IClientHandler<byte[]> handler = (byte[] replyBytes) -> {
        QueryReply reply = new QueryReply();
        try {
            reply.readFromBytes(replyBytes);
        } catch (IOException ex) {
            reply.setStatus(ReplyStatus.ERROR);
            reply.setErrorMsg(ex.getMessage());
        }
        queue.add(reply);
    };
}
