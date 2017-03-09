/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.riac.handlers;

import it.av.fac.messaging.client.ReplyStatus;
import it.av.fac.messaging.client.StorageReply;
import it.av.fac.messaging.client.StorageRequest;
import it.av.fac.messaging.interfaces.IClientHandler;
import it.av.fac.messaging.interfaces.IFACConnection;
import it.av.fac.messaging.interfaces.IServerHandler;
import it.av.fac.messaging.rabbitmq.RabbitMQClient;
import it.av.fac.messaging.rabbitmq.RabbitMQInternalConstants;
import it.av.fac.messaging.rabbitmq.RabbitMQConnectionWrapper;
import it.av.fac.messaging.rabbitmq.RabbitMQServer;
import it.av.fac.messaging.rabbitmq.test.Server;
import it.av.fac.riac.classifier.IClassifier;
import java.io.IOException;
import java.util.concurrent.SynchronousQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class responsible for handling RIaC requests.
 *
 * @author Diogo Regateiro
 */
public class RIaCHandler implements IServerHandler<byte[], String> {

    private final IClassifier classifier;
    private final SynchronousQueue<StorageReply> queue = new SynchronousQueue<>();
    private final RabbitMQClient conn;

    public RIaCHandler(IClassifier classifier) throws Exception {
        System.out.println("Using classifier: " + classifier.getClass().getSimpleName());
        this.classifier = classifier;
        this.conn = new RabbitMQClient(RabbitMQConnectionWrapper.getInstance(),
                RabbitMQInternalConstants.QUEUE_DBI_RESPONSE,
                RabbitMQInternalConstants.QUEUE_DBI_REQUEST, RIaCConfig.MODULE_KEY, handler);
    }

    @Override
    public void handle(byte[] requestBytes, String clientKey) {
        try (IFACConnection clientConn = new RabbitMQServer(
                RabbitMQConnectionWrapper.getInstance(),
                RabbitMQInternalConstants.QUEUE_RIAC_RESPONSE, clientKey)) {
            StorageReply reply = handle(new StorageRequest().readFromBytes(requestBytes));
            System.out.println("Replying with " + reply.getStatus().name() + " : " + reply.getErrorMsg());
            clientConn.send(reply.convertToBytes());
        } catch (Exception ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private StorageReply handle(StorageRequest request) {
        StorageReply errorReply = new StorageReply();
        errorReply.setStatus(ReplyStatus.ERROR);

        // classify the document in the request
        System.out.println("Classifying " + request.getAditionalInfo().getOrDefault("title", "no title"));
        classifier.classify(request);
        return requestStorage(request);
    }

    /**
     * Classify and send the document to the DBI.
     *
     * @param request The request with the document to classify and store.
     * @return The storage process status.
     */
    private StorageReply requestStorage(StorageRequest request) {
        StorageReply errorReply = new StorageReply();

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
        StorageReply reply = new StorageReply();
        try {
            reply.readFromBytes(replyBytes);
        } catch (IOException ex) {
            reply.setStatus(ReplyStatus.ERROR);
            reply.setErrorMsg(ex.getMessage());
        }
        queue.add(reply);
    };
}
