/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.webserver.handlers;

import it.av.fac.messaging.client.ReplyStatus;
import it.av.fac.messaging.client.StorageReply;
import it.av.fac.messaging.client.StorageRequest;
import it.av.fac.messaging.client.StorageRequest.StorageRequestType;
import it.av.fac.messaging.interfaces.IClientHandler;
import it.av.fac.messaging.interfaces.IFACConnection;
import it.av.fac.messaging.rabbitmq.RabbitMQClient;
import it.av.fac.messaging.rabbitmq.RabbitMQInternalConstants;
import it.av.fac.messaging.rabbitmq.RabbitMQConnectionWrapper;
import it.av.fac.webserver.WebserverConfig;
import java.io.IOException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeoutException;

/**
 * Class responsible for handling AdminAPI requests.
 *
 * @author Diogo Regateiro
 */
public class StorageHandler implements Handler<StorageRequest, StorageReply> {

    @Override
    public StorageReply handle(StorageRequest request) {
        StorageReply errorReply = new StorageReply();

        try (RabbitMQConnectionWrapper connWrapper = RabbitMQConnectionWrapper.getInstance()) {
            SynchronousQueue<StorageReply> queue = new SynchronousQueue<>();

            IClientHandler<byte[]> handler = (byte[] replyBytes) -> {
                StorageReply reply = new StorageReply();
                try {
                    reply.readFromBytes(replyBytes);
                } catch (IOException ex) {
                    reply.setStatus(ReplyStatus.ERROR);
                    reply.setErrorMsg(ex.getMessage());
                }
                queue.add(reply);
            };

            try (IFACConnection conn = new RabbitMQClient(connWrapper,
                    RabbitMQInternalConstants.QUEUE_RIAC_RESPONSE,
                    RabbitMQInternalConstants.QUEUE_RIAC_REQUEST, WebserverConfig.MODULE_KEY, handler)) {
                conn.send(request.convertToBytes());
                return queue.take();
            } catch (Exception ex) {
                errorReply.setStatus(ReplyStatus.ERROR);
                errorReply.setErrorMsg(ex.getMessage());
            }
        } catch (NumberFormatException | IOException | TimeoutException ex) {
            errorReply.setStatus(ReplyStatus.ERROR);
            errorReply.setErrorMsg(ex.getMessage());
        }

        return errorReply;
    }
}
