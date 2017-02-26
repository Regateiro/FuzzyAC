/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.messaging.rabbitmq.test;

import it.av.fac.messaging.interfaces.IFACConnection;
import it.av.fac.messaging.rabbitmq.RabbitMQPublicConstants;
import it.av.fac.messaging.rabbitmq.RabbitMQServer;
import java.io.FileInputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import it.av.fac.messaging.interfaces.IServerHandler;
import java.io.IOException;

/**
 *
 * @author Regateiro
 */
public class Server {

    public static void main(String[] args) {
        try {
            Properties msgProperties = new Properties();
            msgProperties.load(new FileInputStream("messaging.properties"));

            String addr = msgProperties.getProperty("provider.addr", "127.0.0.1");
            int port = Integer.valueOf(msgProperties.getProperty("provider.port", "5672"));
            String username = msgProperties.getProperty("provider.auth.user", "guest");
            String password = msgProperties.getProperty("provider.auth.pass", "guest");

            IServerHandler<Integer, String> handler = (Integer request, String clientKey) -> {
                try (IFACConnection<Integer, Integer> clientConn = new RabbitMQServer<>(
                        addr, port, username, password, RabbitMQPublicConstants.QUEUE_QUERY_RESPONSE, clientKey)) {
                    System.out.println("Received request from [" + clientKey + "] for value [" + request + "]");
                    clientConn.send(request >> 1);
                } catch (Exception ex) {
                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                }
            };

            try (IFACConnection<Integer, Integer> serverConn = new RabbitMQServer<>(
                    addr, port, username, password, RabbitMQPublicConstants.QUEUE_QUERY_REQUEST, handler)) {
                System.in.read();
            } catch (Exception ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (IOException | NumberFormatException ex) {
            Logger.getLogger(RabbitMQServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
