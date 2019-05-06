/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.information.handlers;

import it.av.fac.messaging.client.BDFISReply;
import it.av.fac.messaging.client.BDFISRequest;
import it.av.fac.messaging.client.FACLogger;
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
import it.av.wikipedia.client.util.WikipediaUtil;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.SynchronousQueue;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Class responsible for handling RIaC requests.
 *
 * @author Diogo Regateiro <diogoregateiro@ua.pt>
 */
public class InformationHandler implements IServerHandler<byte[], String> {

    private final SynchronousQueue<IReply> queue = new SynchronousQueue<>();
    private final RabbitMQClient conn;
    private final SimpleDateFormat fromWikiDF, toWikiDF;
    private final FACLogger logger;

    public InformationHandler(FACLogger logger) throws Exception {
        this.fromWikiDF = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        this.toWikiDF = new SimpleDateFormat("yyyyMMddHHmmss");
        this.logger = logger;
        this.conn = new RabbitMQClient(RabbitMQConnectionWrapper.getInstance(),
                RabbitMQConstants.QUEUE_DBI_RESPONSE,
                RabbitMQConstants.QUEUE_DBI_REQUEST, InformationConfig.MODULE_KEY, handler);
    }

    @Override
    public void handle(byte[] requestBytes, String clientKey) {
        try (IFACConnection clientConn = new RabbitMQServer(
                RabbitMQConnectionWrapper.getInstance(),
                RabbitMQConstants.QUEUE_INFORMATION_RESPONSE, clientKey)) {
            IReply reply = handle(BDFISRequest.readFromBytes(requestBytes));
            clientConn.send(reply.convertToBytes());
        } catch (Exception ex) {
            logger.error(ex.getMessage());
        }
    }

    private IReply handle(IRequest request) {
        // validate information and refresh it if necessary
        switch (request.getRequestType()) {
            case GetSubjectInfo:
            case GetUserContributions:
                return requestSubjectInfo(request);
            case AddSubject:
                IReply reply = registerSubject(request);
                if (reply.getStatus() == ReplyStatus.OK) {
                    JSONObject user = new JSONObject(reply.getData().get(0));
                    IReply contribReply;
                    if ((contribReply = updateSubjectInformation(user)).getStatus() == ReplyStatus.ERROR) {
                        return contribReply;
                    }
                }
                return reply;
            default:
                String errorMsg = "Invalid request type for the Information module.";
                logger.error(errorMsg);
                return new BDFISReply(ReplyStatus.ERROR, errorMsg);
        }
    }

    /**
     * Registers a new subject data.
     *
     * @param request The request with the subject information.
     * @return The storage process status.
     */
    private IReply registerSubject(IRequest request) {
        IReply reply = new BDFISReply();
        String token = UUID.randomUUID().toString();

        WikipediaUtil util = new WikipediaUtil();
        JSONObject user = util.GetUserByName((String) request.getResourceId()).getJSONObject(0);
        int userid = user.getInt("userid");
        logger.info("Requested wikipedia id for user " + (String) request.getResourceId() + ": " + userid);

        if (!user.keySet().contains("missing")) {
            try {
                logger.info("Registering user.");
                IRequest checkRequest = new BDFISRequest(null, user.getInt("userid"), RequestType.GetSubjectInfo);
                IReply subjectInfo = requestSubjectInfo(checkRequest);
                long tokenExpirationTS = new JSONObject(subjectInfo.getData().get(0)).getJSONObject("token_expires").getLong("$numberLong");

                if (subjectInfo.getData().isEmpty() || new Date().after(new Date(tokenExpirationTS))) {
                    user.put("_id", userid);
                    user.put("token", token);
                    user.put("token_expires", System.currentTimeMillis() + (1000 * 60 * 60 * 24));
                    user.put("attributes", new JSONObject());
                    user.remove("userid");
                    request.setResource(user.toString());
                    conn.send(request.convertToBytes());
                    IReply registerReply = queue.take();
                    if (registerReply.getStatus() == ReplyStatus.ERROR) {
                        return registerReply;
                    }
                    reply.addData(user.toString());
                } else {
                    reply.addData(new JSONObject(subjectInfo.getData().get(0)).toString());
                }
            } catch (IOException | InterruptedException | JSONException ex) {
                logger.error(ex.getMessage());
                reply = new BDFISReply(ReplyStatus.ERROR, ex.getMessage());
            }
        } else {
            String errorMsg = "No user was found.";
            logger.error(errorMsg);
            reply = new BDFISReply(ReplyStatus.ERROR, errorMsg);
        }

        return reply;
    }

    /**
     * Registers a new subject data.
     *
     * @param request The request with the subject information.
     * @return The storage process status.
     */
    private IReply updateSubjectInformation(JSONObject user) {
        IReply reply;

        logger.info("Updating user information: " + user.toString());
        String token = user.getString("token");
        int userid = user.getInt("_id");

        // update the subject attributes
        try {
            JSONObject subjectAttributes = getUserAttributes(userid);
            JSONObject subjectUpdate = new JSONObject().put("_id", userid).put("attributes", subjectAttributes);
            conn.send(new BDFISRequest(token, userid, RequestType.AddSubject).setResource(subjectUpdate.toString()).convertToBytes());

            if ((reply = queue.take()).getStatus() == ReplyStatus.ERROR) {
                return reply;
            }
        } catch (IOException | InterruptedException | JSONException ex) {
            logger.error(ex.getMessage());
        }

        // determine the continue code
        String continueCode = null;
        try {
            conn.send(new BDFISRequest(token, userid, RequestType.GetLastUserContribution).setResource(
                    new JSONObject().put("timestamp", -1).toString()
            ).convertToBytes());

            if ((reply = queue.take()).getStatus() == ReplyStatus.ERROR) {
                return reply;
            }

            if (!reply.getData().isEmpty()) {
                JSONObject contrib = new JSONObject(reply.getData().get(0));
                continueCode = String.format("%s|%d",
                        this.toWikiDF.format(new Date(contrib.getLong("timestamp"))),
                        contrib.getInt("_id"));
            }
        } catch (IOException | InterruptedException | JSONException ex) {
            logger.error(ex.getMessage());
        }

        logger.info("Requesting new contributions.");
        // Query Wikipedia for all new contributions and add them to mongo
        WikipediaUtil util = new WikipediaUtil();
        JSONArray batchContribs = util.GetAllUserContributions(String.valueOf(userid), continueCode, null);

        do {
            for (int i = 0; i < batchContribs.length(); i++) {
                try {
                    JSONObject contrib = batchContribs.getJSONObject(i);
                    contrib.put("_id", contrib.getInt("revid"));
                    contrib.put("timestamp", fromWikiDF.parse(contrib.getString("timestamp")));
                    contrib.remove("revid");

                    BDFISRequest contribStorageRequest = new BDFISRequest(token, userid, RequestType.AddUserContribution);
                    contribStorageRequest.setResource(contrib.toString());

                    try {
                        conn.send(contribStorageRequest.convertToBytes());
                        if ((reply = queue.take()).getStatus() == ReplyStatus.ERROR) {
                            return reply;
                        }
                    } catch (IOException | InterruptedException ex) {
                        logger.error(ex.getMessage());
                        reply = new BDFISReply(ReplyStatus.ERROR, ex.getMessage());
                    }
                } catch (ParseException ex) {
                    String errorMsg = "Error while getting contributions.";
                    logger.error(errorMsg);
                    reply = new BDFISReply(ReplyStatus.ERROR, errorMsg);
                }
            }
        } while (!((batchContribs = util.next()).length() == 0));

        return new BDFISReply();
    }

    /**
     * Requests subject data.
     *
     * @param request The request the subject identification.
     * @return The storage process status.
     */
    private IReply requestSubjectInfo(IRequest request) {
        IReply reply;

        try {
            conn.send(request.convertToBytes());
            return queue.take();
        } catch (IOException | InterruptedException ex) {
            reply = new BDFISReply(ReplyStatus.ERROR, ex.getMessage());
        }

        return reply;
    }

    private final IClientHandler<byte[]> handler = (byte[] replyBytes) -> {
        IReply reply;
        try {
            reply = BDFISReply.readFromBytes(replyBytes);
        } catch (IOException ex) {
            reply = new BDFISReply(ReplyStatus.ERROR, ex.getMessage());
        }
        queue.add(reply);
    };

    /**
     * TODO: Use actual data sources. Currently adds random attributes.
     *
     * @param userid
     * @return
     */
    private JSONObject getUserAttributes(int userid) {
        JSONObject attr = new JSONObject();

        attr.put("Number_Of_Publications", Math.floor(Math.random() * 20));
        attr.put("Number_Of_Citations", Math.floor(Math.random() * 100));
        attr.put("Role", Math.floor(Math.random() * 4) + 1);
        attr.put("Years_Of_Service", Math.floor(Math.random() * 20));
        attr.put("Years_Partened", Math.floor(Math.random() * 20));
        attr.put("Number_Of_Projects_Funded", Math.floor(Math.random() * 20));

        return attr;
    }
}
