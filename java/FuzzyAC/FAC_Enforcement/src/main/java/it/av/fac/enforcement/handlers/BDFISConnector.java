/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.enforcement.handlers;

import it.av.fac.enforcement.util.EnforcementConfig;
import it.av.fac.messaging.client.BDFISDecision;
import it.av.fac.messaging.client.BDFISReply;
import it.av.fac.messaging.client.BDFISRequest;
import it.av.fac.messaging.client.FACLogger;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.SynchronousQueue;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Class responsible for handling query requests.
 *
 * @author Diogo Regateiro <diogoregateiro@ua.pt>
 */
public class BDFISConnector {

    private static BDFISConnector instance;
    private final SynchronousQueue<IReply> decisionQueue = new SynchronousQueue<>();
    private final SynchronousQueue<IReply> dbiQueue = new SynchronousQueue<>();
    private final SynchronousQueue<IReply> riacQueue = new SynchronousQueue<>();
    private final SynchronousQueue<IReply> infoQueue = new SynchronousQueue<>();
    private final RabbitMQClient decisionConn;
    private final RabbitMQClient dbiConn;
    private final RabbitMQClient riacConn;
    private final RabbitMQClient infoConn;
    private final FACLogger logger;

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
        this.infoConn = new RabbitMQClient(RabbitMQConnectionWrapper.getInstance(),
                RabbitMQConstants.QUEUE_INFORMATION_RESPONSE,
                RabbitMQConstants.QUEUE_INFORMATION_REQUEST, EnforcementConfig.MODULE_KEY, infoHandler);
        this.logger = new FACLogger("Webservice");
    }

    public static BDFISConnector getInstance() throws Exception {
        if (instance == null) {
            instance = new BDFISConnector();
        }

        return instance;
    }
    
    public FACLogger getLogger() {
        return logger;
    }

    /**
     * TODO: cache the decisions for some time.
     *
     * @param resource
     * @param userToken
     * @param permission
     * @return
     */
    public JSONObject filterPage(JSONObject resource, String userToken, String permission) {
        System.out.println("Processing query for: " + resource);

        System.out.println("Requesting resource metadata...");
        IRequest metadataRequest = new BDFISRequest(userToken, resource.getString("_id"), RequestType.GetMetadata);
        IReply resourceMetaReply = requestResource(metadataRequest);

        System.out.println("Requesting access control decision for each security label...");
        List<IReply> decisionsReplies = new ArrayList<>();
        Set<String> pageSecurityLabels = new HashSet<>();
        resourceMetaReply.getData().stream().forEach((String metadataStr) -> {
            JSONObject metadata = new JSONObject(metadataStr);

            JSONArray sections = metadata.getJSONArray("text");
            for (int i = 0; i < sections.length(); i++) {
                JSONObject section = sections.getJSONObject(i);
                pageSecurityLabels.add(section.getString("security_label"));
            }

            pageSecurityLabels.forEach((pageSecurityLabel) -> {
                IRequest decisionRequest = new BDFISRequest(userToken, pageSecurityLabel, RequestType.Decision);
                decisionsReplies.add(requestDecision(decisionRequest));
            });
        });

        System.out.println("Parsing decisions...");
        Map<String, BDFISDecision> decisions = new HashMap<>();
        decisionsReplies.stream().forEach((IReply decisionReply) -> {
            decisionReply.getData().stream().forEach((String decisionStr) -> {
                BDFISDecision decision = BDFISDecision.readFromString(decisionStr);
                if (decision.getPermission().equalsIgnoreCase("*") || decision.getPermission().equalsIgnoreCase(permission)) {
                    decisions.put(decision.getSecurityLabel(), decision);
                }
            });
        });

        System.out.println("Filtering headings according to access control decisions...");
        JSONArray sections = resource.getJSONArray("text");
        JSONArray filteredSections = new JSONArray();
        for (int i = 0; i < sections.length(); i++) {
            JSONObject section = sections.getJSONObject(i);
            BDFISDecision decision = decisions.get(section.getString("security_label"));
            if (decision.isGranted()) {
                filteredSections.put(section);
            }
        }
        resource.put("text", filteredSections);

        return resource;
    }

    /**
     * TODO: cache the decisions for some time.
     *
     * @param resource
     * @param userToken
     * @return
     */
    public JSONObject flagWritableSections(JSONObject resource, String userToken) {
        System.out.println("Processing query for: " + resource);

        System.out.println("Requesting resource metadata...");
        IRequest metadataRequest = new BDFISRequest(userToken, resource.getString("_id"), RequestType.GetMetadata);
        IReply resourceMetaReply = requestResource(metadataRequest);

        System.out.println("Requesting access control decision for each security label...");
        List<IReply> decisionsReplies = new ArrayList<>();
        Set<String> pageSecurityLabels = new HashSet<>();
        resourceMetaReply.getData().stream().forEach((String metadataStr) -> {
            JSONObject metadata = new JSONObject(metadataStr);

            JSONArray sections = metadata.getJSONArray("text");
            for (int i = 0; i < sections.length(); i++) {
                JSONObject section = sections.getJSONObject(i);
                pageSecurityLabels.add(section.getString("security_label"));
            }

            pageSecurityLabels.forEach((pageSecurityLabel) -> {
                IRequest decisionRequest = new BDFISRequest(userToken, pageSecurityLabel, RequestType.Decision);
                decisionsReplies.add(requestDecision(decisionRequest));
            });
        });

        System.out.println("Parsing decisions...");
        Map<String, BDFISDecision> decisions = new HashMap<>();
        decisionsReplies.stream().forEach((IReply decisionReply) -> {
            decisionReply.getData().stream().forEach((String decisionStr) -> {
                BDFISDecision decision = BDFISDecision.readFromString(decisionStr);
                if (decision.getPermission().equalsIgnoreCase("*") || decision.getPermission().equalsIgnoreCase("write")) {
                    decisions.put(decision.getSecurityLabel(), decision);
                }
            });
        });

        System.out.println("Flagging headings according to access control decisions...");
        JSONArray sections = resource.getJSONArray("text");
        for (int i = 0; i < sections.length(); i++) {
            JSONObject section = sections.getJSONObject(i);
            BDFISDecision decision = decisions.get(section.getString("security_label"));
            section.put("editable", decision.isGranted());
        }

        return resource;
    }

    public boolean store(JSONObject resource, String userToken) {
        System.out.println("Processing store request...");
        IRequest storeRequest = new BDFISRequest(userToken, resource.getString("_id"), RequestType.AddMetadata);
        storeRequest.setResource(resource.toString());
        IReply storeReply = requestResourceStorage(storeRequest);
        return storeReply.getStatus() == ReplyStatus.OK;
    }

    /**
     * Registers a user and returns a token. Returns a token if it has been
     * registered already.
     *
     * @param userName
     * @return
     */
    public String registerUser(String userName) {
        System.out.println("Processing user register request...");
        IRequest storeRequest = new BDFISRequest(null, userName, RequestType.AddSubject);
        IReply storeReply = requestUserRegistration(storeRequest);
        if (storeReply.getStatus() == ReplyStatus.OK) {
            return storeReply.getData().get(0);
        }
        return null;
    }

    /**
     * Registers a user and returns a token. Returns a token if it has been
     * registered already.
     *
     * @param token
     * @return
     */
    public JSONObject getUserInfo(String token) {
        System.out.println("Processing user info retrieval request...");
        IRequest request = new BDFISRequest(token, null, RequestType.GetSubjectInfo);
        IReply reply = requestUserInfo(request);
        if (reply.getStatus() == ReplyStatus.OK) {
            return new JSONObject(reply.getData().get(0));
        }
        return null;
    }

    /**
     * Request documents to the DBI.
     *
     * @param request The request with the document to classify and store.
     * @return The storage process status.
     */
    private IReply requestUserInfo(IRequest request) {
        IReply reply;

        try {
            infoConn.send(request.convertToBytes());
            return infoQueue.take();
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
    private IReply requestResource(IRequest request) {
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
    private IReply requestUserRegistration(IRequest request) {
        IReply reply;

        try {
            infoConn.send(request.convertToBytes());
            return infoQueue.take();
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

    private final IClientHandler<byte[]> infoHandler = (byte[] replyBytes) -> {
        IReply reply;
        try {
            reply = BDFISReply.readFromBytes(replyBytes);
        } catch (IOException ex) {
            reply = new BDFISReply(ReplyStatus.ERROR, ex.getMessage());
        }
        infoQueue.add(reply);
    };
}
