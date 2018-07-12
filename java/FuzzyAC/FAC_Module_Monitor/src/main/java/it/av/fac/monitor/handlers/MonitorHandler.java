/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.monitor.handlers;

import it.av.fac.messaging.client.BDFISReply;
import it.av.fac.messaging.client.BDFISRequest;
import it.av.fac.messaging.client.ReplyStatus;
import it.av.fac.messaging.client.interfaces.IReply;
import it.av.fac.messaging.client.interfaces.IRequest;
import it.av.fac.messaging.interfaces.IFACConnection;
import it.av.fac.messaging.interfaces.IServerHandler;
import it.av.fac.messaging.rabbitmq.RabbitMQConnectionWrapper;
import it.av.fac.messaging.rabbitmq.RabbitMQConstants;
import it.av.fac.messaging.rabbitmq.RabbitMQServer;
import it.av.fac.messaging.rabbitmq.test.Server;
import it.av.fac.monitor.BDFISLogger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Diogo Regateiro <diogoregateiro@ua.pt>
 */
public class MonitorHandler implements IServerHandler<byte[], String> {

    private final BDFISLogger logger;

    public MonitorHandler(BDFISLogger logger) throws Exception {
        this.logger = logger;
    }

    @Override
    public void handle(byte[] requestBytes, String clientKey) {
        try (IFACConnection clientConn = new RabbitMQServer(
                RabbitMQConnectionWrapper.getInstance(),
                RabbitMQConstants.QUEUE_MONITOR_RESPONSE, clientKey)) {
            IReply reply = handle(BDFISRequest.readFromBytes(requestBytes));
            clientConn.send(reply.convertToBytes());
        } catch (Exception ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private IReply handle(IRequest request) {
        // validate information and refresh it if necessary
        switch (request.getRequestType()) {
            case LogError:
                logger.log(BDFISLogger.LogLevel.Error, request.getUserToken(), request.getResource());
                break;
            case LogWarning:
                logger.log(BDFISLogger.LogLevel.Warning, request.getUserToken(), request.getResource());
                break;
            case LogInfo:
                logger.log(BDFISLogger.LogLevel.Info, request.getUserToken(), request.getResource());
                break;
            default:
                String errorMsg = "Invalid request type for the Monitor module: " + request.getRequestType();
                logger.log(BDFISLogger.LogLevel.Error, MonitorConfig.MODULE_KEY, errorMsg);
                return new BDFISReply(ReplyStatus.ERROR, errorMsg);
        }
        
        return new BDFISReply();
    }
}
