/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.webserver.handlers;

import it.av.fac.messaging.client.ReplyStatus;
import it.av.fac.messaging.client.StorageReply;
import it.av.fac.messaging.client.StorageRequest;
import it.av.fac.messaging.interfaces.IClientHandler;
import it.av.fac.messaging.interfaces.IFACConnection;
import it.av.fac.messaging.rabbitmq.RabbitMQClient;
import it.av.fac.messaging.rabbitmq.RabbitMQConstants;
import it.av.fac.messaging.rabbitmq.RabbitMQConnectionWrapper;
import it.av.fac.webserver.WebserverConfig;
import java.io.IOException;
import java.util.concurrent.SynchronousQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class responsible for handling AdminAPI requests.
 *
 * @author Diogo Regateiro
 */
public class StorageHandler implements Handler<StorageRequest, StorageReply> {

    private static StorageHandler instance;
    private final SynchronousQueue<StorageReply> queue = new SynchronousQueue<>();
    private final RabbitMQClient conn;

    private StorageHandler() throws Exception {
        this.conn = new RabbitMQClient(RabbitMQConnectionWrapper.getInstance(),
                RabbitMQConstants.QUEUE_RIAC_RESPONSE,
                RabbitMQConstants.QUEUE_RIAC_REQUEST, WebserverConfig.MODULE_KEY, handler);
    }
    
    public static StorageHandler getInstance() throws Exception {
        if(instance == null) {
            instance = new StorageHandler();
        }
        
        return instance;
    }

    @Override
    public StorageReply handle(StorageRequest request) {
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

    private final IClientHandler<byte[]> handler = (final byte[] replyBytes) -> {
        StorageReply reply = new StorageReply();
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
