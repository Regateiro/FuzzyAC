/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.dbi.handlers;

import it.av.fac.dbi.drivers.DocumentDBI;
import it.av.fac.messaging.client.BDFISReply;
import it.av.fac.messaging.client.BDFISRequest;
import it.av.fac.messaging.client.FACLogger;
import it.av.fac.messaging.client.ReplyStatus;
import it.av.fac.messaging.client.interfaces.IReply;
import it.av.fac.messaging.client.interfaces.IRequest;
import it.av.fac.messaging.interfaces.IFACConnection;
import it.av.fac.messaging.interfaces.IServerHandler;
import it.av.fac.messaging.rabbitmq.RabbitMQConstants;
import it.av.fac.messaging.rabbitmq.RabbitMQConnectionWrapper;
import it.av.fac.messaging.rabbitmq.RabbitMQServer;
import java.io.IOException;
import org.json.JSONObject;

/**
 * Class responsible for handling RIaC requests.
 *
 * @author Diogo Regateiro <diogoregateiro@ua.pt>
 */
public class DBIHandler implements IServerHandler<byte[], String> {

    private final FACLogger logger;

    public DBIHandler(FACLogger logger) {
        this.logger = logger;
    }

    @Override
    public void handle(byte[] requestBytes, String clientKey) {
        try (IFACConnection clientConn = new RabbitMQServer(
                RabbitMQConnectionWrapper.getInstance(),
                RabbitMQConstants.QUEUE_DBI_RESPONSE, clientKey)) {
            IReply reply = handle(BDFISRequest.readFromBytes(requestBytes));
            clientConn.send(reply.convertToBytes());
        } catch (Exception ex) {
            logger.error(ex.getMessage());
        }
    }

    private IReply handle(IRequest request) {
        switch (request.getRequestType()) {
            case GetPolicy:
                return requestResource(request, "policies");
            case AddPolicy:
                return storeResource(request, "policies");
            case GetMetadata:
                return requestResource(request, "metadata");
            case AddMetadata:
                return storeResource(request, "metadata");
            case GetSubjectInfo:
                return requestResource(request, "wikiusers");
            case AddSubject:
                return storeResource(request, "wikiusers");
            case GetUserContributions:
                return requestResource(request, "contribs");
            case AddUserContribution:
                return storeResource(request, "contribs");
            case GetLastUserContribution:
                return requestLastResource(request, "contribs");
            default:
                String errorMsg = "Invalid request type for the DBI module.";
                logger.error(errorMsg);
                return new BDFISReply(ReplyStatus.ERROR, errorMsg);
        }
    }

    /**
     * Stores a resource on the datastore.
     *
     * @param request The request with the document to store.
     * @return The storage process status.
     */
    private IReply storeResource(IRequest request, String collection) {
        IReply reply = new BDFISReply();
        try {
            JSONObject resource = new JSONObject(request.getResource());
            logger.info("Storing resource in collection " + collection + ": " + resource.toString());
            DocumentDBI.getInstance(collection).storeResource(resource);
        } catch (IOException ex) {
            logger.error(ex.getMessage());
            reply = new BDFISReply(ReplyStatus.ERROR, ex.getMessage());
        }
        return reply;
    }

    /**
     * Request resources from the datastore.
     *
     * @param request The request with the document id to retrieve.
     * @return The storage process status.
     */
    private IReply requestResource(IRequest request, String collection) {
        IReply reply = new BDFISReply();
        try {
            Object id = request.getResourceId();
            logger.info("Requesting resource " + id + " from collection " + collection);
            if (id != null) {
                reply = DocumentDBI.getInstance(collection).findResource(id);
            } else if (request.getResource() != null) {
                reply = DocumentDBI.getInstance(collection).findResource(new JSONObject(request.getResource()));
            } else {
                reply = DocumentDBI.getInstance(collection).findResource(new JSONObject().put("token", request.getUserToken()));
            }
        } catch (IOException ex) {
            logger.error(ex.getMessage());
            reply = new BDFISReply(ReplyStatus.ERROR, ex.getMessage());
        }
        return reply;
    }

    /**
     * Request resources from the datastore.
     *
     * @param request The request with the document id to retrieve.
     * @return The storage process status.
     */
    private IReply requestLastResource(IRequest request, String collection) {
        IReply reply;
        try {
            logger.info("Requesting last resource from collection " + collection + ": " + request.getResource());
            reply = DocumentDBI.getInstance(collection).findLastResource(request.getResourceId(), new JSONObject(request.getResource()));
        } catch (IOException ex) {
            logger.error(ex.getMessage());
            reply = new BDFISReply(ReplyStatus.ERROR, ex.getMessage());
        }
        return reply;
    }
}
