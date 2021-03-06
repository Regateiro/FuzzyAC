/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.messaging.rabbitmq.test;

import it.av.fac.messaging.interfaces.IClientHandler;
import it.av.fac.messaging.interfaces.IFACConnection;
import it.av.fac.messaging.rabbitmq.RabbitMQClient;
import it.av.fac.messaging.rabbitmq.RabbitMQConnectionWrapper;
import it.av.fac.messaging.rabbitmq.RabbitMQConstants;
import it.av.fac.messaging.rabbitmq.RabbitMQServer;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONObject;

/**
 *
 * @author Regateiro
 */
public class Client {

    private static final String CLIENT_KEY = "messaging.1";

    public static void main(String[] args) {
        System.out.println(CLIENT_KEY);
        
        try (RabbitMQConnectionWrapper connWrapper = RabbitMQConnectionWrapper.getInstance()){
            IClientHandler<byte[]> handler = (byte[] reply) -> {
            };

            try (IFACConnection conn = new RabbitMQClient(connWrapper,
                    RabbitMQConstants.QUEUE_QUERY_RESPONSE,
                    RabbitMQConstants.QUEUE_QUERY_REQUEST, CLIENT_KEY, handler)) {
                JSONObject request = new JSONObject();
                request.put("request", "ping");
                while (true) {
                    conn.send(request.toString().getBytes());
                }
            } catch (Exception ex) {
                Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (NumberFormatException | IOException | TimeoutException ex) {
            Logger.getLogger(RabbitMQServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
