/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.dbi.handlers;

import it.av.fac.dbi.drivers.DocumentDBI;
import it.av.fac.dbi.drivers.GraphDBI;
import it.av.fac.messaging.client.ReplyStatus;
import it.av.fac.messaging.client.DBIReply;
import it.av.fac.messaging.client.DBIRequest;
import it.av.fac.messaging.client.DBIRequest.DBIRequestType;
import it.av.fac.messaging.interfaces.IFACConnection;
import it.av.fac.messaging.interfaces.IServerHandler;
import it.av.fac.messaging.rabbitmq.RabbitMQConstants;
import it.av.fac.messaging.rabbitmq.RabbitMQConnectionWrapper;
import it.av.fac.messaging.rabbitmq.RabbitMQServer;
import it.av.fac.messaging.rabbitmq.test.Server;
import java.io.IOException;
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
                RabbitMQConstants.QUEUE_DBI_RESPONSE, clientKey)) {
            DBIReply reply = handle(new DBIRequest().readFromBytes(requestBytes));
            clientConn.send(reply.convertToBytes());
        } catch (Exception ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private DBIReply handle(DBIRequest request) {
        DBIReply reply = new DBIReply();
        reply.setStatus(ReplyStatus.ERROR);

        DBIRequestType requestType = DBIRequestType.valueOf(request.getRequestType().name());

        switch (requestType) {
            case QueryPolicies:
                return requestPolicies(request);
            case QueryDocuments:
                return requestDocuments(request);
            case StoreGraphNode:
                return requestNodeStorage(request);
            case StoreDocument:
                return requestDocumentStorage(request);
        }

        reply.setErrorMsg("Not a known request type was received: " + requestType);
        return reply;

    }

    /**
     * Forwards the request to store a node in a graph.
     *
     * @param request The request with the node store.
     * @return The storage process status.
     */
    private DBIReply requestNodeStorage(DBIRequest request) {
        DBIReply reply = new DBIReply();
        try {
            GraphDBI graphDBI = new GraphDBI();
            graphDBI.storeNode(request);
            reply.setStatus(ReplyStatus.OK);
        } catch (IOException ex) {
            reply.setErrorMsg(ex.getMessage());
            reply.setStatus(ReplyStatus.ERROR);
        }
        return reply;
    }

    /**
     * Classify and send the document to the DBI.
     *
     * @param request The request with the document to classify and store.
     * @return The storage process status.
     */
    private DBIReply requestDocumentStorage(DBIRequest request) {
        DBIReply reply = new DBIReply();
        try {
            DocumentDBI.getInstance(request.getStorageId()).storeDocument(request);
            reply.setStatus(ReplyStatus.OK);
        } catch (IOException ex) {
            reply.setErrorMsg(ex.getMessage());
            reply.setStatus(ReplyStatus.ERROR);
        }
        return reply;
    }

    /**
     * Request documents from the datastore.
     *
     * @param request The request with the document to classify and store.
     * @return The storage process status.
     */
    private DBIReply requestDocuments(DBIRequest request) {
        DBIReply reply = new DBIReply();
        try {
            reply = DocumentDBI.getInstance(request.getStorageId()).query(request);
        } catch (IOException ex) {
            reply.setErrorMsg(ex.getMessage());
            reply.setStatus(ReplyStatus.ERROR);
        }
        return reply;
    }

    /**
     * Request documents from the datastore.
     *
     * @param request The request with the document to classify and store.
     * @return The storage process status.
     */
    private DBIReply requestPolicies(DBIRequest request) {
        DBIReply reply = new DBIReply();
        try {
            reply = DocumentDBI.getInstance(request.getStorageId()).find(request);
        } catch (IOException ex) {
            reply.setErrorMsg(ex.getMessage());
            reply.setStatus(ReplyStatus.ERROR);
        }
        return reply;
    }
}
