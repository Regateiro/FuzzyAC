/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.messaging.rabbitmq;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author Diogo Regateiro
 */
public class RabbitMQConnectionWrapper implements Closeable {

    private static RabbitMQConnectionWrapper instance = null;
    private final Connection conn;

    private RabbitMQConnectionWrapper() throws FileNotFoundException, IOException, TimeoutException {
        Properties msgProperties = new Properties();
        msgProperties.load(new FileInputStream("messaging.properties"));

        String addr = msgProperties.getProperty("provider.addr", "127.0.0.1");
        int port = Integer.valueOf(msgProperties.getProperty("provider.port", "5672"));
        String username = msgProperties.getProperty("provider.auth.user", "guest");
        String password = msgProperties.getProperty("provider.auth.pass", "guest");
        
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(addr);
        factory.setPort(port);
        factory.setUsername(username);
        factory.setPassword(password);
        this.conn = factory.newConnection();
    }

    public static RabbitMQConnectionWrapper getInstance() throws IOException, FileNotFoundException, TimeoutException {
        if (instance == null) {
            instance = new RabbitMQConnectionWrapper();
        }

        return instance;
    }

    public Connection getConnection() {
        return conn;
    }

    @Override
    public void close() throws IOException {
        conn.close();
        instance = null;
    }
    
}
