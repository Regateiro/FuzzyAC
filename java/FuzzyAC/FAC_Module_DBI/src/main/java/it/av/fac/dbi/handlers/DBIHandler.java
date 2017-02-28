/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.dbi.handlers;

import it.av.fac.messaging.client.ReplyStatus;
import it.av.fac.messaging.client.StorageReply;
import it.av.fac.messaging.client.StorageRequest;
import it.av.fac.messaging.client.StorageRequest.StorageRequestType;
import it.av.fac.messaging.interfaces.IClientHandler;
import it.av.fac.messaging.interfaces.IFACConnection;
import it.av.fac.messaging.interfaces.IServerHandler;
import it.av.fac.messaging.rabbitmq.RabbitMQClient;
import it.av.fac.messaging.rabbitmq.RabbitMQInternalConstants;
import it.av.fac.messaging.rabbitmq.RabbitMQConnectionWrapper;
import it.av.fac.messaging.rabbitmq.RabbitMQServer;
import it.av.fac.messaging.rabbitmq.test.Server;
import java.io.IOException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class responsible for handling RIaC requests.
 *
 * @author Diogo Regateiro
 */
public class DBIHandler implements IServerHandler<byte[], String> {

    @Override
    public void handle(byte[] requestBytes, String clientKey) {
        try (IFACConnection clientConn = new RabbitMQServer(
                RabbitMQConnectionWrapper.getInstance(),
                RabbitMQInternalConstants.QUEUE_DBI_RESPONSE, clientKey)) {
            clientConn.send(handle(new StorageRequest().readFromBytes(requestBytes)).convertToBytes());
        } catch (Exception ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private StorageReply handle(StorageRequest request) {
        StorageReply errorReply = new StorageReply();
        errorReply.setStatus(ReplyStatus.ERROR);

        StorageRequestType requestType = StorageRequestType.valueOf(request.getRequestType().name());

        switch (requestType) {
            case StoreGraphNode:
                return requestNodeStorage(request);
            case StoreDocument:
                return requestDocumentStorage(request);
        }

        errorReply.setErrorMsg("Not a known request type was received: " + requestType);
        return errorReply;

    }

    /**
     * Forwards the request to store a node in a graph.
     * @param request The request with the node store.
     * @return The storage process status.
     */
    private StorageReply requestNodeStorage(StorageRequest request) {
        StorageReply reply = new StorageReply();

        // access the database and store.
        
        return reply;
    }

    /**
     * Classify and send the document to the DBI.
     * @param request The request with the document to classify and store.
     * @return The storage process status.
     */
    private StorageReply requestDocumentStorage(StorageRequest request) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
