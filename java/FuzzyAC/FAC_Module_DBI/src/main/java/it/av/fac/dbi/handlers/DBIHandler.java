/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.dbi.handlers;

import com.alibaba.fastjson.JSONObject;
import it.av.fac.dbi.drivers.DocumentDBI;
import it.av.fac.messaging.client.BDFISReply;
import it.av.fac.messaging.client.BDFISRequest;
import it.av.fac.messaging.client.ReplyStatus;
import it.av.fac.messaging.client.interfaces.IReply;
import it.av.fac.messaging.client.interfaces.IRequest;
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
            IReply reply = handle(BDFISRequest.readFromBytes(requestBytes));
            clientConn.send(reply.convertToBytes());
        } catch (Exception ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private IReply handle(IRequest request) {
        switch (request.getRequestType()) {
            case GetPolicy:
                return requestPolicy(request);
            case AddPolicy:
                return requestPolicyStorage(request);
            case GetMetadata:
                return requestMetadata(request);
            case AddMetadata:
                return requestMetadataStorage(request);
        }

        return new BDFISReply(ReplyStatus.ERROR, "Not a known request type was received: " + request.getRequestType());

    }

    /**
     * Classify and send the document to the DBI.
     *
     * @param request The request with the document to classify and store.
     * @return The storage process status.
     */
    private IReply requestMetadataStorage(IRequest request) {
        IReply reply = new BDFISReply();
        try {
            DocumentDBI.getInstance("metadata").storeResource(JSONObject.parseObject(request.getResource()));
        } catch (IOException ex) {
            reply = new BDFISReply(ReplyStatus.ERROR, ex.getMessage());
        }
        return reply;
    }

    /**
     * Request documents from the datastore.
     *
     * @param request The request with the document to classify and store.
     * @return The storage process status.
     */
    private IReply requestMetadata(IRequest request) {
        IReply reply = new BDFISReply();
        try {
            reply = DocumentDBI.getInstance("metadata").findResource(request.getResourceId());
        } catch (IOException ex) {
            reply = new BDFISReply(ReplyStatus.ERROR, ex.getMessage());
        }
        return reply;
    }

    private IReply requestPolicyStorage(IRequest request) {
        IReply reply = new BDFISReply();
        try {
            DocumentDBI.getInstance("policies").storeResource(JSONObject.parseObject(request.getResource()));
        } catch (IOException ex) {
            reply = new BDFISReply(ReplyStatus.ERROR, ex.getMessage());
        }
        return reply;
    }

    private IReply requestPolicy(IRequest request) {
        IReply reply = new BDFISReply();
        try {
            reply = DocumentDBI.getInstance("policies").findResource(request.getResourceId());
        } catch (IOException ex) {
            reply = new BDFISReply(ReplyStatus.ERROR, ex.getMessage());
        }
        return reply;
    }
}
