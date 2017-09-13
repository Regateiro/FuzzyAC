/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.policyretrieval.handlers;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import it.av.fac.messaging.client.DBIReply;
import it.av.fac.messaging.client.DBIRequest;
import it.av.fac.messaging.client.PolicyReply;
import it.av.fac.messaging.client.PolicyRequest;
import it.av.fac.messaging.client.ReplyStatus;
import it.av.fac.messaging.interfaces.IClientHandler;
import it.av.fac.messaging.interfaces.IFACConnection;
import it.av.fac.messaging.interfaces.IServerHandler;
import it.av.fac.messaging.rabbitmq.RabbitMQClient;
import it.av.fac.messaging.rabbitmq.RabbitMQConstants;
import it.av.fac.messaging.rabbitmq.RabbitMQConnectionWrapper;
import it.av.fac.messaging.rabbitmq.RabbitMQServer;
import it.av.fac.messaging.rabbitmq.test.Server;
import it.av.fac.policyretrieval.util.PolicyRetrievalConfig;
import java.io.IOException;
import java.util.concurrent.SynchronousQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class responsible for handling query requests.
 *
 * @author Diogo Regateiro
 */
public class PolicyRetrievalHandler implements IServerHandler<byte[], String> {

    private final SynchronousQueue<DBIReply> dbiQueue = new SynchronousQueue<>();
    private final RabbitMQClient dbiConn;

    public PolicyRetrievalHandler() throws Exception {
        this.dbiConn = new RabbitMQClient(RabbitMQConnectionWrapper.getInstance(),
                RabbitMQConstants.QUEUE_DBI_RESPONSE,
                RabbitMQConstants.QUEUE_DBI_REQUEST, PolicyRetrievalConfig.MODULE_KEY, dbiHandler);
    }

    @Override
    public void handle(byte[] requestBytes, String clientKey) {
        try (IFACConnection clientConn = new RabbitMQServer(
                RabbitMQConnectionWrapper.getInstance(),
                RabbitMQConstants.QUEUE_POLICY_RETRIEVAL_RESPONSE, clientKey)) {
            PolicyReply reply = handle(new PolicyRequest().readFromBytes(requestBytes));
            clientConn.send(reply.convertToBytes());
        } catch (Exception ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private PolicyReply handle(PolicyRequest policyRequest) {
        PolicyReply policyReply = new PolicyReply();
        policyReply.setStatus(ReplyStatus.OK);

        System.out.println("Processing policy request");

        System.out.println("Requesting documents...");
        policyRequest.getSecurityLabels().parallelStream().forEach((label) -> {
            DBIRequest dbiRequest = new DBIRequest();
            dbiRequest.setRequestType(DBIRequest.DBIRequestType.QueryPolicies);
            dbiRequest.setStorageId("policies");

            JSONObject fields = new JSONObject();
            fields.put("security_label", label);
            dbiRequest.setMetadata("fields", fields.toJSONString());
            
            DBIReply documentsReply = requestPolicies(dbiRequest);
            documentsReply.getDocuments().stream().forEach((doc) -> addPolicyToReplySync(policyReply, doc));
        });

        System.out.println("Replying with the filtered documents.");
        return policyReply;
    }
    
    private synchronized void addPolicyToReplySync(PolicyReply reply, JSONObject policy) {
        reply.addPolicy(policy);
    }

    /**
     * Request documents to the DBI.
     *
     * @param request The request with the document to classify and store.
     * @return The storage process status.
     */
    private DBIReply requestPolicies(DBIRequest request) {
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
}
