/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.information.handlers;

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
import it.av.wikipedia.client.util.WikipediaUtil;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.SynchronousQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Class responsible for handling RIaC requests.
 *
 * @author Diogo Regateiro
 */
public class InformationHandler implements IServerHandler<byte[], String> {

    private final SynchronousQueue<IReply> queue = new SynchronousQueue<>();
    private final RabbitMQClient conn;
    private final SimpleDateFormat fromWikiDF, fromMongoDBDF;

    public InformationHandler() throws Exception {
        this.fromWikiDF = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        this.fromMongoDBDF = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");
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
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
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
                    if ((contribReply = registerContributions(
                            user.getString("token"),
                            user.getInt("_id")
                    )).getStatus() == ReplyStatus.ERROR) {
                        return contribReply;
                    }
                }
                return reply;
            default:
                return new BDFISReply(ReplyStatus.ERROR, "Invalid request type for the Information module.");
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
        
        if (!user.keySet().contains("missing")) {
            try {
                IRequest checkRequest = new BDFISRequest(null, user.getInt("userid"), RequestType.GetSubjectInfo);
                IReply subjectInfo = requestSubjectInfo(checkRequest);

                if (subjectInfo.getData().isEmpty() || new Date().after(fromMongoDBDF.parse(new JSONObject(subjectInfo.getData().get(0)).getString("token_expires")))) {
                    user.put("_id", userid);
                    user.put("token", token);
                    user.put("token_expires", new Date(System.currentTimeMillis() + (1000*60*60*24)));
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
            } catch (IOException | InterruptedException | ParseException | JSONException ex) {
                reply = new BDFISReply(ReplyStatus.ERROR, ex.getMessage());
            }
        } else {
            reply = new BDFISReply(ReplyStatus.ERROR, "No user was found.");
        }

        return reply;
    }

    /**
     * Registers a new subject data.
     *
     * @param request The request with the subject information.
     * @return The storage process status.
     */
    private IReply registerContributions(String token, int userid) {
        IReply reply;

        WikipediaUtil util = new WikipediaUtil();

        String continueCode = null;
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
                        reply = new BDFISReply(ReplyStatus.ERROR, ex.getMessage());
                    }
                } catch (ParseException ex) {
                    reply = new BDFISReply(ReplyStatus.ERROR, "Error while getting contributions.");
                }
            }
        } while (!((batchContribs = util.next()).length() == 0));

        return new BDFISReply(ReplyStatus.OK, "");
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
}