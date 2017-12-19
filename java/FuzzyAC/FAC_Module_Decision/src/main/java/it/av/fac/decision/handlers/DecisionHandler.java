/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.decision.handlers;

import it.av.fac.decision.fis.BDFIS;
import it.av.fac.decision.util.decision.DecisionConfig;
import it.av.fac.dfcl.DynamicFunction;
import it.av.fac.messaging.client.BDFISDecision;
import it.av.fac.messaging.client.BDFISReply;
import it.av.fac.messaging.client.BDFISRequest;
import it.av.fac.messaging.client.ReplyStatus;
import it.av.fac.messaging.client.RequestType;
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
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.SynchronousQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sourceforge.jFuzzyLogic.rule.Variable;
import org.antlr.runtime.RecognitionException;
import org.json.JSONObject;

/**
 * Class responsible for handling query requests.
 *
 * @author Diogo Regateiro <diogoregateiro@ua.pt>
 */
public class DecisionHandler implements IServerHandler<byte[], String> {

    private final SynchronousQueue<IReply> infoQueue = new SynchronousQueue<>();
    private final SynchronousQueue<IReply> polretQueue = new SynchronousQueue<>();
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
            IReply reply = handle(BDFISRequest.readFromBytes(requestBytes));
            clientConn.send(reply.convertToBytes());
        } catch (Exception ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private IReply handle(IRequest request) {
        IReply decisionReply = new BDFISReply();

        System.out.println("Processing request for user: " + request.getUserToken());
        System.out.println("Requesting the FCL files for the security label..."); //PolicyRetrieval
        String securityLabel = (String) request.getResourceId();

        IRequest polRequest = new BDFISRequest(request.getUserToken(), securityLabel, RequestType.GetPolicy);
        IReply polReply = requestPolicies(polRequest);

        System.out.println("Requesting the user attributes..."); //Information
        IRequest infoRequest = new BDFISRequest(request.getUserToken(), null, RequestType.GetSubjectInfo);
        IReply infoReply = requestInformation(infoRequest);

        Map<String, Double> userVariables = new HashMap<>();
        JSONObject user = new JSONObject(infoReply.getData().get(0));
        JSONObject userAttributes = user.getJSONObject("attributes");
        userVariables.put("Number_Of_Publications", userAttributes.optDouble("Number_Of_Publications", 12.0));
        userVariables.put("Number_Of_Citations", userAttributes.optDouble("Number_Of_Citations", 50.0));
        userVariables.put("Role", userAttributes.optDouble("Role", 0.0));
        userVariables.put("Years_Of_Service", userAttributes.optDouble("Years_Of_Service", 0.0));
        userVariables.put("Years_Partened", userAttributes.optDouble("Years_Partened", 0.0));
        userVariables.put("Number_Of_Projects_Funded", userAttributes.optDouble("Number_Of_Projects_Funded", 0.0));
        
        infoRequest = new BDFISRequest(request.getUserToken(), null, RequestType.GetUserContributions);
        infoRequest.setResource(new JSONObject().put("userid", user.getInt("_id")).toString());
        infoReply = requestInformation(infoRequest);
        DynamicFunction df = new ORES(infoReply.getData());

        System.out.println("Evaluating user permissions...");

        polReply.getData().parallelStream().forEach((fclInfo) -> {
            try {
                String fcl = new JSONObject(fclInfo).getString("fcl");
                if (fcl != null && !fcl.equals("")) {
                    Map<String, Double> neededVariables = new HashMap<>(userVariables);
                    BDFIS feval = new BDFIS(fcl, true);
                    feval.registerDynamicFunction(df);
                    userVariables.keySet().stream().filter((varName) -> !feval.getInputVariableNameList().contains(varName)).forEach((notNeededVarName) -> {
                        neededVariables.remove(notNeededVarName);
                    });
                    Map<String, Variable> evaluation = feval.evaluate(neededVariables, false);
                    evaluation.keySet().stream().forEach((permission) -> {
                        addDecisionToReplySync(decisionReply, securityLabel, permission, evaluation.get(permission).getValue() > alphaCut);
                    });
                } else {
                    addDecisionToReplySync(decisionReply, securityLabel, "*", true);
                }
            } catch (IOException | RecognitionException ex) {
                Logger.getLogger(DecisionHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        });

        System.out.println("Replying...");
        return decisionReply;
    }

    private synchronized void addDecisionToReplySync(IReply reply, String label, String permission, boolean decision) {
        reply.addData(new BDFISDecision(label, permission, decision).convertToString());
    }

    /**
     * Request Information to the Information Module.
     *
     * @param request The request with the information required.
     * @return The information process status.
     */
    private IReply requestInformation(IRequest request) {
        IReply reply;

        try {
            infoConn.send(request.convertToBytes());
            reply = infoQueue.take();
        } catch (IOException | InterruptedException ex) {
            reply = new BDFISReply(ReplyStatus.ERROR, ex.getMessage());
        }

        return reply;
    }

    /**
     * Request a GetPolicy to the GetPolicy Retrieval Module.
     *
     * @param request The request with the document to classify and store.
     * @return The storage process status.
     */
    private IReply requestPolicies(IRequest request) {
        IReply reply;

        try {
            polretConn.send(request.convertToBytes());
            reply = polretQueue.take();
        } catch (IOException | InterruptedException ex) {
            reply = new BDFISReply(ReplyStatus.ERROR, ex.getMessage());
        }

        return reply;
    }

    private final IClientHandler<byte[]> infoHandler = (byte[] replyBytes) -> {
        IReply reply;
        try {
            reply = BDFISReply.readFromBytes(replyBytes);
        } catch (IOException ex) {
            reply = new BDFISReply(ReplyStatus.ERROR, ex.getMessage());
        }
        infoQueue.add(reply);
    };

    private final IClientHandler<byte[]> polretHandler = (byte[] replyBytes) -> {
        IReply reply;
        try {
            reply = BDFISReply.readFromBytes(replyBytes);
        } catch (IOException ex) {
            reply = new BDFISReply(ReplyStatus.ERROR, ex.getMessage());
        }
        polretQueue.add(reply);
    };
}
