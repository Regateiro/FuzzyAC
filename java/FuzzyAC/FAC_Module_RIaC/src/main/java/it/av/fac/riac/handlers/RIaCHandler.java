/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.riac.handlers;

import it.av.fac.messaging.client.BDFISReply;
import it.av.fac.messaging.client.BDFISRequest;
import it.av.fac.messaging.client.FACLogger;
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
import it.av.fac.riac.classifier.IClassifier;
import java.io.IOException;
import java.util.concurrent.SynchronousQueue;
import org.json.JSONObject;

/**
 * Class responsible for handling RIaC requests.
 *
 * @author Diogo Regateiro <diogoregateiro@ua.pt>
 */
public class RIaCHandler implements IServerHandler<byte[], String> {

    private final IClassifier classifier;
    private final SynchronousQueue<IReply> queue = new SynchronousQueue<>();
    private final RabbitMQClient conn;
    private final FACLogger logger;

    public RIaCHandler(IClassifier classifier, FACLogger logger) throws Exception {
        logger.info("Using classifier: " + classifier.getClass().getSimpleName());
        this.classifier = classifier;
        this.logger = logger;
        this.conn = new RabbitMQClient(RabbitMQConnectionWrapper.getInstance(),
                RabbitMQConstants.QUEUE_DBI_RESPONSE,
                RabbitMQConstants.QUEUE_DBI_REQUEST, RIaCConfig.MODULE_KEY, handler);
    }

    @Override
    public void handle(byte[] requestBytes, String clientKey) {
        try (IFACConnection clientConn = new RabbitMQServer(
                RabbitMQConnectionWrapper.getInstance(),
                RabbitMQConstants.QUEUE_RIAC_RESPONSE, clientKey)) {
            IReply reply = handle(BDFISRequest.readFromBytes(requestBytes));
            clientConn.send(reply.convertToBytes());
        } catch (Exception ex) {
            logger.error(ex.getMessage());
        }
    }

    private IReply handle(IRequest request) {
        // classify the document in the request
        logger.info("Classifying " + new JSONObject(request.getResource()).optString("title", "no title"));
        classifier.classify(request);
        return requestStorage(request);
    }

    /**
     * Classify and send the document to the DBI.
     *
     * @param request The request with the document to classify and store.
     * @return The storage process status.
     */
    private IReply requestStorage(IRequest request) {
        IReply reply;

        try {
            conn.send(request.convertToBytes());
            return queue.take();
        } catch (IOException | InterruptedException ex) {
            logger.error(ex.getMessage());
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
