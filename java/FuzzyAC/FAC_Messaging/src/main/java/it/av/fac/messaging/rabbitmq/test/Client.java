/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.messaging.rabbitmq.test;

import it.av.fac.messaging.interfaces.IFACConnection;
import it.av.fac.messaging.rabbitmq.RMQPublicConstants;
import it.av.fac.messaging.rabbitmq.RabbitMQFACConnection;
import java.io.FileInputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.util.Callback;

/**
 *
 * @author Regateiro
 */
public class Client {

    private static IFACConnection<Integer> conn_in, conn_out;

    public static void main(String[] args) {
        try {
            Properties msgProperties = new Properties();
            msgProperties.load(new FileInputStream("messaging.properties"));

            Callback<Integer, Void> callback = (Integer reply) -> {
                //System.out.println(String.format("Server replied with [%s]", reply));
                int newRequest = (int) (Math.random() * (Integer.MAX_VALUE - 1));
                //System.out.println(String.format("Sending new request: [%d].", newRequest));
                conn_out.send(newRequest);
                return null;
            };

            String addr = msgProperties.getProperty("provider.addr", "127.0.0.1");
            int port = Integer.valueOf(msgProperties.getProperty("provider.port", "5672"));
            String username = msgProperties.getProperty("provider.auth.user", "guest");
            String password = msgProperties.getProperty("provider.auth.pass", "guest");

            conn_out = new RabbitMQFACConnection<>(addr, port, username, password, RMQPublicConstants.QUEUE_QUERY_REQUEST);
            conn_in = new RabbitMQFACConnection<>(addr, port, username, password, RMQPublicConstants.QUEUE_QUERY_RESPONSE, callback);

            int newRequest = (int) (Math.random() * (Integer.MAX_VALUE - 1));
            //System.out.println(String.format("Sending new request: [%d].", newRequest));
            conn_out.send(newRequest);

            System.in.read();
            conn_out.close();
            conn_in.close();
        } catch (Exception ex) {
            Logger.getLogger(RabbitMQFACConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
