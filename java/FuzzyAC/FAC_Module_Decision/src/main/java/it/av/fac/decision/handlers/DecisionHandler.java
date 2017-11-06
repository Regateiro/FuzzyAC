/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.decision.handlers;

import it.av.fac.decision.fis.BDFIS;
import it.av.fac.decision.util.decision.DecisionConfig;
import it.av.fac.messaging.client.DecisionReply;
import it.av.fac.messaging.client.DecisionRequest;
import it.av.fac.messaging.client.InformationReply;
import it.av.fac.messaging.client.InformationRequest;
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
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.SynchronousQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sourceforge.jFuzzyLogic.rule.Variable;
import org.antlr.runtime.RecognitionException;

/**
 * Class responsible for handling query requests.
 *
 * @author Diogo Regateiro
 */
public class DecisionHandler implements IServerHandler<byte[], String> {

    private final SynchronousQueue<InformationReply> infoQueue = new SynchronousQueue<>();
    private final SynchronousQueue<PolicyReply> polretQueue = new SynchronousQueue<>();
    private final RabbitMQClient infoConn;
    private final RabbitMQClient polretConn;
    private final double alphaCut = 0.5;

    public DecisionHandler() throws Exception {
        this.infoConn = new RabbitMQClient(RabbitMQConnectionWrapper.getInstance(),
                RabbitMQConstants.QUEUE_INFORMATION_RESPONSE,
                RabbitMQConstants.QUEUE_INFORMATION_REQUEST, DecisionConfig.MODULE_KEY, infoHandler);
        this.polretConn = new RabbitMQClient(RabbitMQConnectionWrapper.getInstance(),
                RabbitMQConstants.QUEUE_POLICY_RETRIEVAL_RESPONSE,
                RabbitMQConstants.QUEUE_POLICY_RETRIEVAL_REQUEST, DecisionConfig.MODULE_KEY, polretHandler);
    }

    @Override
    public void handle(byte[] requestBytes, String clientKey) {
        try (IFACConnection clientConn = new RabbitMQServer(
                RabbitMQConnectionWrapper.getInstance(),
                RabbitMQConstants.QUEUE_DECISION_RESPONSE, clientKey)) {
            DecisionReply reply = handle(new DecisionRequest().readFromBytes(requestBytes));
            clientConn.send(reply.convertToBytes());
        } catch (Exception ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private DecisionReply handle(DecisionRequest request) {
        DecisionReply decisionReply = new DecisionReply();
        decisionReply.setStatus(ReplyStatus.OK);

        System.out.println("Processing request for user: " + request.getUserToken());
        System.out.println("Requesting the FCL files for the security labels..."); //PolicyRetrieval
        PolicyRequest polRequest = new PolicyRequest();
        polRequest.setRequestType(PolicyRequest.PolicyRetrievalRequestType.BySecurityLabel);
        polRequest.setSecurityLabels(request.getSecurityLabels());
        PolicyReply polReply = requestPolicies(polRequest);

        System.out.println("Requesting the user attributes..."); //Information
        Map<String, Double> userVariables = new HashMap<>();
        userVariables.put("Number_Of_Publications", 12.0);
        userVariables.put("Number_Of_Citations", 50.0);
        userVariables.put("Role", 0.0);
        userVariables.put("Years_Of_Service", 0.0);
        userVariables.put("Years_Partened", 0.0);
        userVariables.put("Number_Of_Projects_Funded", 0.0);

        System.out.println("Evaluating user permissions...");

        polReply.getPolicies().parallelStream().forEach((policy) -> {
            try {
                String fcl = policy.getString("document");
                if (fcl != null) {
                    Map<String, Double> neededVariables = new HashMap<>(userVariables);
                    BDFIS feval = new BDFIS(fcl, true);
                    userVariables.keySet().stream().filter((varName) -> !feval.getVariableNameList().contains(varName)).forEach((notNeededVarName) -> {
                            neededVariables.remove(notNeededVarName);
                    });
                    Map<String, Variable> evaluation = feval.evaluate(neededVariables, false);
                    evaluation.keySet().stream().forEach((permission) -> {
                        addDecisionToReplySync(decisionReply, policy.getString("security_label"), permission, evaluation.get(permission).getValue() > alphaCut);
                    });
                } else {
                    addDecisionToReplySync(decisionReply, policy.getString("security_label"), "*", true);
                }
            } catch (RecognitionException ex) {
                Logger.getLogger(DecisionHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        });

        System.out.println("Replying...");
        return decisionReply;
    }

    private synchronized void addDecisionToReplySync(DecisionReply reply, String label, String permission, boolean decision) {
        reply.addDecision(label, permission, decision);
    }

    /**
     * Request Information to the Information Module.
     *
     * @param request The request with the information required.
     * @return The information process status.
     */
    private InformationReply requestInformation(InformationRequest request) {
        InformationReply reply = new InformationReply();

        try {
            infoConn.send(request.convertToBytes());
            reply = infoQueue.take();
        } catch (IOException | InterruptedException ex) {
            reply.setStatus(ReplyStatus.ERROR);
            reply.setErrorMsg(ex.getMessage());
        }

        return reply;
    }

    /**
     * Request a Policy to the Policy Retrieval Module.
     *
     * @param request The request with the document to classify and store.
     * @return The storage process status.
     */
    private PolicyReply requestPolicies(PolicyRequest request) {
        PolicyReply reply = new PolicyReply();

        try {
            polretConn.send(request.convertToBytes());
            reply = polretQueue.take();
        } catch (IOException | InterruptedException ex) {
            reply.setStatus(ReplyStatus.ERROR);
            reply.setErrorMsg(ex.getMessage());
        }

        return reply;
    }

    private final IClientHandler<byte[]> infoHandler = (byte[] replyBytes) -> {
        InformationReply reply = new InformationReply();
        try {
            reply.readFromBytes(replyBytes);
        } catch (IOException ex) {
            reply.setStatus(ReplyStatus.ERROR);
            reply.setErrorMsg(ex.getMessage());
        }
        infoQueue.add(reply);
    };

    private final IClientHandler<byte[]> polretHandler = (byte[] replyBytes) -> {
        PolicyReply reply = new PolicyReply();
        try {
            reply.readFromBytes(replyBytes);
        } catch (IOException ex) {
            reply.setStatus(ReplyStatus.ERROR);
            reply.setErrorMsg(ex.getMessage());
        }
        polretQueue.add(reply);
    };
}
