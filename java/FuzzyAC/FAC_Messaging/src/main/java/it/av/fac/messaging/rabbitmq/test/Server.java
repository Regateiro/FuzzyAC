/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.messaging.rabbitmq.test;

import it.av.fac.messaging.interfaces.IFACConnection;
import it.av.fac.messaging.rabbitmq.RabbitMQConstants;
import it.av.fac.messaging.rabbitmq.RabbitMQServer;
import java.util.logging.Level;
import java.util.logging.Logger;
import it.av.fac.messaging.interfaces.IServerHandler;
import it.av.fac.messaging.rabbitmq.RabbitMQConnectionWrapper;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import org.json.JSONObject;

/**
 *
 * @author Regateiro
 */
public class Server {

    public static void main(String[] args) {
        try (RabbitMQConnectionWrapper connWrapper = RabbitMQConnectionWrapper.getInstance()){
            IServerHandler<byte[], String> handler = (byte[] request, String clientKey) -> {
                try (IFACConnection clientConn = new RabbitMQServer(
                        connWrapper, RabbitMQConstants.QUEUE_QUERY_RESPONSE, clientKey)) {
                    JSONObject reply = new JSONObject();
                    reply.put("reply", "pong");
                    reply.put("status", "ok");
                    clientConn.send(reply.toString().getBytes());
                } catch (Exception ex) {
                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                }
            };

            try (IFACConnection serverConn = new RabbitMQServer(
                    connWrapper, RabbitMQConstants.QUEUE_QUERY_REQUEST, handler)) {
                System.in.read();
            } catch (Exception ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (NumberFormatException | IOException | TimeoutException ex) {
            Logger.getLogger(RabbitMQServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
