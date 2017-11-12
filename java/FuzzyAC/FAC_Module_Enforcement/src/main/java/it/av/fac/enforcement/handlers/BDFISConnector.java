/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.enforcement.handlers;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import it.av.fac.enforcement.util.EnforcementConfig;
import it.av.fac.messaging.client.BDFISDecision;
import it.av.fac.messaging.client.BDFISReply;
import it.av.fac.messaging.client.BDFISRequest;
import it.av.fac.messaging.client.ReplyStatus;
import it.av.fac.messaging.client.RequestType;
import it.av.fac.messaging.client.interfaces.IReply;
import it.av.fac.messaging.client.interfaces.IRequest;
import it.av.fac.messaging.interfaces.IClientHandler;
import it.av.fac.messaging.rabbitmq.RabbitMQClient;
import it.av.fac.messaging.rabbitmq.RabbitMQConstants;
import it.av.fac.messaging.rabbitmq.RabbitMQConnectionWrapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.SynchronousQueue;

/**
 * Class responsible for handling query requests.
 *
 * @author Diogo Regateiro
 */
public class BDFISConnector {

    private static BDFISConnector instance;
    private final SynchronousQueue<IReply> decisionQueue = new SynchronousQueue<>();
    private final SynchronousQueue<IReply> dbiQueue = new SynchronousQueue<>();
    private final SynchronousQueue<IReply> riacQueue = new SynchronousQueue<>();
    private final RabbitMQClient decisionConn;
    private final RabbitMQClient dbiConn;
    private final RabbitMQClient riacConn;

    public BDFISConnector() throws Exception {
        this.decisionConn = new RabbitMQClient(RabbitMQConnectionWrapper.getInstance(),
                RabbitMQConstants.QUEUE_DECISION_RESPONSE,
                RabbitMQConstants.QUEUE_DECISION_REQUEST, EnforcementConfig.MODULE_KEY, decisionHandler);
        this.dbiConn = new RabbitMQClient(RabbitMQConnectionWrapper.getInstance(),
                RabbitMQConstants.QUEUE_DBI_RESPONSE,
                RabbitMQConstants.QUEUE_DBI_REQUEST, EnforcementConfig.MODULE_KEY, dbiHandler);
        this.riacConn = new RabbitMQClient(RabbitMQConnectionWrapper.getInstance(),
                RabbitMQConstants.QUEUE_RIAC_RESPONSE,
                RabbitMQConstants.QUEUE_RIAC_REQUEST, EnforcementConfig.MODULE_KEY, riacHandler);
    }

    public static BDFISConnector getInstance() throws Exception {
        if (instance == null) {
            instance = new BDFISConnector();
        }

        return instance;
    }

    /**
     * TODO: cache the decisions for some time.
     *
     * @param resource
     * @param userToken
     * @param permission
     * @param mustBeGrantedForEveryLabel
     * @return
     */
    public boolean canAccess(String resource, String userToken, String permission, boolean mustBeGrantedForEveryLabel) {
        System.out.println("Processing query for: " + resource);

        System.out.println("Requesting resource metadata...");
        IRequest metadataRequest = new BDFISRequest(userToken, resource, RequestType.Metadata);
        IReply resourceMetaReply = requestMetadata(metadataRequest);

        System.out.println("Requesting access control decision for each security label...");
        List<IReply> decisionsReplies = new ArrayList<>();
        resourceMetaReply.getData().stream().forEach((String securityLabel) -> {
            IRequest decisionRequest = new BDFISRequest(userToken, securityLabel, RequestType.Decision);
            decisionsReplies.add(requestDecision(decisionRequest));
        });

        System.out.println("Parsing decisions...");
        List<BDFISDecision> decisions = new ArrayList<>();
        decisionsReplies.stream().forEach((IReply decisionReply) -> {
            decisionReply.getData().stream().forEach((String decisionStr) -> {
                BDFISDecision decision = BDFISDecision.readFromString(decisionStr);
                if (decision.getPermission().equalsIgnoreCase(permission)) {
                    decisions.add(decision);
                }
            });
        });

        System.out.println("Determining the final decision for the permission...");
        boolean allLabelsGranted = true;
        boolean allLabelsDenied = true;
        for (BDFISDecision decision : decisions) {
            if(!decision.isGranted()) {
                allLabelsGranted = false;
            }
            
            if(decision.isGranted()) {
                allLabelsDenied = false;
            }
        }

        System.out.println("Replying with the final decision.");
        return allLabelsGranted || (!mustBeGrantedForEveryLabel && !allLabelsDenied);
    }
    
    public boolean store(JSONObject resource, String userToken) {
        System.out.println("Processing store request...");
        IRequest storeRequest = new BDFISRequest(userToken, resource.getString("_id"), RequestType.AddPolicy);
        storeRequest.setResource(resource.toJSONString());
        IReply storeReply = requestResourceStorage(storeRequest);
        return storeReply.getStatus() == ReplyStatus.OK;
    }

    /**
     * Request documents to the DBI.
     *
     * @param request The request with the document to classify and store.
     * @return The storage process status.
     */
    private IReply requestMetadata(IRequest request) {
        IReply reply;

        try {
            dbiConn.send(request.convertToBytes());
            return dbiQueue.take();
        } catch (IOException | InterruptedException ex) {
            reply = new BDFISReply(ReplyStatus.ERROR, ex.getMessage());
        }

        return reply;
    }

    /**
     * Request documents to the DBI.
     *
     * @param request The request with the document to classify and store.
     * @return The storage process status.
     */
    private IReply requestDecision(IRequest request) {
        IReply reply;

        try {
            decisionConn.send(request.convertToBytes());
            reply = decisionQueue.take();
        } catch (IOException | InterruptedException ex) {
            reply = new BDFISReply(ReplyStatus.ERROR, ex.getMessage());
        }

        return reply;
    }
    
    /**
     * Request documents to the DBI.
     *
     * @param request The request with the document to classify and store.
     * @return The storage process status.
     */
    private IReply requestResourceStorage(IRequest request) {
        IReply reply;

        try {
            riacConn.send(request.convertToBytes());
            return riacQueue.take();
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

    private final IClientHandler<byte[]> decisionHandler = (byte[] replyBytes) -> {
        IReply reply;
        try {
            reply = BDFISReply.readFromBytes(replyBytes);
        } catch (IOException ex) {
            reply = new BDFISReply(ReplyStatus.ERROR, ex.getMessage());
        }
        decisionQueue.add(reply);
    };
    
    private final IClientHandler<byte[]> riacHandler = (byte[] replyBytes) -> {
        IReply reply;
        try {
            reply = BDFISReply.readFromBytes(replyBytes);
        } catch (IOException ex) {
            reply = new BDFISReply(ReplyStatus.ERROR, ex.getMessage());
        }
        riacQueue.add(reply);
    };
}
