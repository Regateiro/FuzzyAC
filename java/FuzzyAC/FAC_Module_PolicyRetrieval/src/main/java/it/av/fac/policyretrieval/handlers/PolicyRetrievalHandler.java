/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.policyretrieval.handlers;

import it.av.fac.messaging.client.BDFISReply;
import it.av.fac.messaging.client.BDFISRequest;
import it.av.fac.messaging.client.ReplyStatus;
import it.av.fac.messaging.client.interfaces.IReply;
import it.av.fac.messaging.client.interfaces.IRequest;
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

    private final SynchronousQueue<IReply> dbiQueue = new SynchronousQueue<>();
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
            IReply reply = handle(BDFISRequest.readFromBytes(requestBytes));
            clientConn.send(reply.convertToBytes());
        } catch (Exception ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private IReply handle(IRequest policyRequest) {
        IReply policyReply = new BDFISReply(ReplyStatus.OK, "");

        System.out.println("Processing policy request");

        System.out.println("Requesting documents...");
        String policy = (String) policyRequest.getResourceId();

        // do stuff with the policy perhaps
        
        IReply documentsReply = requestPolicy(policyRequest);
        documentsReply.getData().stream().forEach((doc) -> addPolicyToReplySync(policyReply, doc));

        System.out.println("Replying with the filtered documents.");
        return policyReply;
    }

    private synchronized void addPolicyToReplySync(IReply reply, String policy) {
        reply.addData(policy);
    }

    /**
     * Request documents to the DBI.
     *
     * @param request The request with the document to classify and store.
     * @return The storage process status.
     */
    private IReply requestPolicy(IRequest request) {
        IReply reply;

        try {
            dbiConn.send(request.convertToBytes());
            return dbiQueue.take();
        } catch (IOException | InterruptedException ex) {
            reply = new BDFISReply(ReplyStatus.ERROR, ex.getMessage());
        }

        return reply;
    }

    private final IClientHandler<byte[]> dbiHandler = (byte[] replyBytes) -> {
        IReply reply;
        try {
            reply = BDFISReply.readFromBytes(replyBytes);
        } catch (IOException ex) {
            reply = new BDFISReply(ReplyStatus.ERROR, ex.getMessage());
        }
        dbiQueue.add(reply);
    };
}
