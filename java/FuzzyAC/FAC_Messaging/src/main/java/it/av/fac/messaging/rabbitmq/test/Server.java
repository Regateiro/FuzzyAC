/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.messaging.rabbitmq.test;

import com.alibaba.fastjson.JSONObject;
import it.av.fac.messaging.interfaces.IFACConnection;
import it.av.fac.messaging.rabbitmq.RabbitMQPublicConstants;
import it.av.fac.messaging.rabbitmq.RabbitMQServer;
import java.util.logging.Level;
import java.util.logging.Logger;
import it.av.fac.messaging.interfaces.IServerHandler;
import it.av.fac.messaging.rabbitmq.RabbitMQConnectionWrapper;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author Regateiro
 */
public class Server {

    public static void main(String[] args) {
        try (RabbitMQConnectionWrapper connWrapper = RabbitMQConnectionWrapper.getInstance()){
            IServerHandler<String, String> handler = (String request, String clientKey) -> {
                try (IFACConnection clientConn = new RabbitMQServer(
                        connWrapper, RabbitMQPublicConstants.QUEUE_QUERY_RESPONSE, clientKey)) {
                    JSONObject reply = new JSONObject();
                    reply.put("reply", "pong");
                    reply.put("status", "ok");
                    clientConn.send(reply.toJSONString());
                } catch (Exception ex) {
                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                }
            };

            try (IFACConnection serverConn = new RabbitMQServer(
                    connWrapper, RabbitMQPublicConstants.QUEUE_QUERY_REQUEST, handler)) {
                System.in.read();
            } catch (Exception ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (NumberFormatException | IOException | TimeoutException ex) {
            Logger.getLogger(RabbitMQServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
