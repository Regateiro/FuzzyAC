/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.enforcement.handlers;

import com.alibaba.fastjson.JSONObject;
import it.av.fac.enforcement.util.EnforcementConfig;
import it.av.fac.messaging.client.DBIReply;
import it.av.fac.messaging.client.DBIRequest;
import it.av.fac.messaging.client.DecisionReply;
import it.av.fac.messaging.client.DecisionRequest;
import it.av.fac.messaging.client.QueryReply;
import it.av.fac.messaging.client.QueryRequest;
import it.av.fac.messaging.client.ReplyStatus;
import it.av.fac.messaging.interfaces.IClientHandler;
import it.av.fac.messaging.rabbitmq.RabbitMQClient;
import it.av.fac.messaging.rabbitmq.RabbitMQConstants;
import it.av.fac.messaging.rabbitmq.RabbitMQConnectionWrapper;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.SynchronousQueue;

/**
 * Class responsible for handling query requests.
 *
 * @author Diogo Regateiro
 */
public class FACEnforcer {

    private static FACEnforcer instance;
    private final SynchronousQueue<DecisionReply> decisionQueue = new SynchronousQueue<>();
    private final SynchronousQueue<DBIReply> dbiQueue = new SynchronousQueue<>();
    private final RabbitMQClient decisionConn;
    private final RabbitMQClient dbiConn;

    public FACEnforcer() throws Exception {
        this.decisionConn = new RabbitMQClient(RabbitMQConnectionWrapper.getInstance(),
                RabbitMQConstants.QUEUE_DECISION_RESPONSE,
                RabbitMQConstants.QUEUE_DECISION_REQUEST, EnforcementConfig.MODULE_KEY, decisionHandler);
        this.dbiConn = new RabbitMQClient(RabbitMQConnectionWrapper.getInstance(),
                RabbitMQConstants.QUEUE_DBI_RESPONSE,
                RabbitMQConstants.QUEUE_DBI_REQUEST, EnforcementConfig.MODULE_KEY, dbiHandler);
    }
    
    public static FACEnforcer getInstance() throws Exception {
        if (instance == null) {
            instance = new FACEnforcer();
        }

        return instance;
    }

    public QueryReply handle(QueryRequest queryRequest) {
        QueryReply queryReply = new QueryReply();
        queryReply.setStatus(ReplyStatus.OK);

        System.out.println("Processing query: " + queryRequest.getQuery());

        System.out.println("Requesting documents...");
        DBIRequest dbiRequest = new DBIRequest();
        dbiRequest.setRequestType(DBIRequest.DBIRequestType.QueryDocuments);
        dbiRequest.setQuery(queryRequest.getQuery());
        dbiRequest.setStorageId(queryRequest.getTargetData());
        DBIReply documentsReply = requestDocuments(dbiRequest);

        System.out.println("Requesting access control decision for each document security label...");
        DecisionRequest decisionRequest = new DecisionRequest();
        decisionRequest.setRequestType(DecisionRequest.DecisionRequestType.Normal);
        decisionRequest.setUserToken(queryRequest.getToken());
        documentsReply.getDocuments().stream().forEach((JSONObject doc) -> {
            //System.out.println(doc.getString("title"));
            decisionRequest.addSecurityLabel(doc.getString("security_label"));
        });

        DecisionReply decisionReply = requestDecision(decisionRequest);

        System.out.println("Filtering documents to which the user does not have read permission...");
        documentsReply.getDocuments().stream().filter((JSONObject doc) -> {
            String label = doc.getString("security_label");
            if (!decisionReply.getSecurityLabels().contains(label)) {
                throw new IllegalStateException("A security label for which a decision was required is not present in the DecisionReply.");
            }
            Map<String, Boolean> decision = decisionReply.getSecurityLabelDecision(label);
            return (decision.keySet().contains("*") && decision.get("*")) || decision.get("Read");
        }).forEach((JSONObject userAccessibleDocument) -> {
            //System.out.println(userAccessibleDocument.getString("title"));
            queryReply.addDocument(userAccessibleDocument);
        });

        System.out.println("Replying with the filtered documents.");
        return queryReply;

    }

    /**
     * Request documents to the DBI.
     *
     * @param request The request with the document to classify and store.
     * @return The storage process status.
     */
    private DBIReply requestDocuments(DBIRequest request) {
        DBIReply reply = new DBIReply();

        try {
            dbiConn.send(request.convertToBytes());
            return dbiQueue.take();
        } catch (IOException | InterruptedException ex) {
            reply.setStatus(ReplyStatus.ERROR);
            reply.setErrorMsg(ex.getMessage());
        }

        return reply;
    }

    /**
     * Request documents to the DBI.
     *
     * @param request The request with the document to classify and store.
     * @return The storage process status.
     */
    private DecisionReply requestDecision(DecisionRequest request) {
        DecisionReply reply = new DecisionReply();

        try {
            decisionConn.send(request.convertToBytes());
            reply = decisionQueue.take();
        } catch (IOException | InterruptedException ex) {
            reply.setStatus(ReplyStatus.ERROR);
            reply.setErrorMsg(ex.getMessage());
        }

        return reply;
    }

    private final IClientHandler<byte[]> dbiHandler = (byte[] replyBytes) -> {
        DBIReply reply = new DBIReply();
        try {
            reply.readFromBytes(replyBytes);
        } catch (IOException ex) {
            reply.setStatus(ReplyStatus.ERROR);
            reply.setErrorMsg(ex.getMessage());
        }
        dbiQueue.add(reply);
    };

    private final IClientHandler<byte[]> decisionHandler = (byte[] replyBytes) -> {
        DecisionReply reply = new DecisionReply();
        try {
            reply.readFromBytes(replyBytes);
        } catch (IOException ex) {
            reply.setStatus(ReplyStatus.ERROR);
            reply.setErrorMsg(ex.getMessage());
        }
        decisionQueue.add(reply);
    };
}
