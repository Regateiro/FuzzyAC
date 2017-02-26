/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.messaging.rabbitmq.test;

import it.av.fac.messaging.interfaces.IClientHandler;
import it.av.fac.messaging.interfaces.IFACConnection;
import it.av.fac.messaging.rabbitmq.RabbitMQClient;
import it.av.fac.messaging.rabbitmq.RabbitMQPublicConstants;
import it.av.fac.messaging.rabbitmq.RabbitMQServer;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Regateiro
 */
public class Client {

    private static final String CLIENT_KEY = "messaging.5";

    public static void main(String[] args) {
        System.out.println(CLIENT_KEY);
        
        try {
            Properties msgProperties = new Properties();
            msgProperties.load(new FileInputStream("messaging.properties"));

            String addr = msgProperties.getProperty("provider.addr", "127.0.0.1");
            int port = Integer.valueOf(msgProperties.getProperty("provider.port", "5672"));
            String username = msgProperties.getProperty("provider.auth.user", "guest");
            String password = msgProperties.getProperty("provider.auth.pass", "guest");

            IClientHandler<Integer> handler = (Integer reply) -> {
                System.out.println(" :: Received [" + reply + "]");
            };

            try (IFACConnection<Integer, Integer> conn = new RabbitMQClient<>(
                    addr, port, username, password, RabbitMQPublicConstants.QUEUE_QUERY_RESPONSE,
                    RabbitMQPublicConstants.QUEUE_QUERY_REQUEST, CLIENT_KEY, handler)) {
                while (true) {
                    int request = (int) (Math.random() * 50) * 2;
                    System.out.print("Sent [" + request + "]");
                    conn.send(request);
                    Thread.sleep((long)(Math.random() * 1000) + 1000);
                }
            } catch (Exception ex) {
                Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (IOException | NumberFormatException ex) {
            Logger.getLogger(RabbitMQServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
