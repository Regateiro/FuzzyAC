/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.webserver.handlers;

import it.av.fac.messaging.client.ReplyStatus;
import it.av.fac.messaging.client.DBIReply;
import it.av.fac.messaging.client.DBIRequest;
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
public class StorageHandler implements Handler<DBIRequest, DBIReply> {

    private static StorageHandler instance;
    private final SynchronousQueue<DBIReply> queue = new SynchronousQueue<>();
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
    public DBIReply handle(DBIRequest request) {
        DBIReply reply = new DBIReply();

        try {
            conn.send(request.convertToBytes());
            return queue.take();
        } catch (IOException | InterruptedException ex) {
            reply.setStatus(ReplyStatus.ERROR);
            reply.setErrorMsg(ex.getMessage());
        }

        return reply;
    }

    private final IClientHandler<byte[]> handler = (final byte[] replyBytes) -> {
        DBIReply reply = new DBIReply();
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
