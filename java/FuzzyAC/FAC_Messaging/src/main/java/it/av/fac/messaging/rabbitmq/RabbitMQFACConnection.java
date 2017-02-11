/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.messaging.rabbitmq;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import java.io.Serializable;
import it.av.fac.messaging.interfaces.IFACConnection;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Properties;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.util.Callback;

/**
 * Allows to send and receive messages within the FAC architecture.
 *
 * @author Diogo Regateiro
 * @param <T> The type of the message to send/receive.
 */
public class RabbitMQFACConnection<T extends Serializable> implements IFACConnection<T> {

    private final Connection connection;
    private final Channel channel;
    private final String QUEUE;

    /**
     * Initializes the FACConnection object.
     *
     * @param addr
     * @param port
     * @param username
     * @param password
     * @param queue
     * @param callback Callback to execute when new messages are available. 
     * @throws java.io.IOException If the connection to the rabbitmq server
     * fails.
     */
    public RabbitMQFACConnection(String addr, int port, String username, String password, String queue, Callback<T, Void> callback) throws Exception {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(addr);
            factory.setPort(port);
            factory.setUsername(username);
            factory.setPassword(password);

            this.QUEUE = queue;

            this.connection = factory.newConnection();
            this.channel = connection.createChannel();
            this.channel.queueDeclare(this.QUEUE, false, false, false, null);

            if (callback != null) {
                this.channel.basicConsume(this.QUEUE, true, new DefaultConsumer(this.channel) {
                    @Override
                    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
                            throws IOException {
                        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(body))) {
                            callback.call((T) ois.readObject());
                        } catch (Exception ex) {
                            Logger.getLogger(RabbitMQFACConnection.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                });
            }
    }

    @Override
    public void send(T message) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(message);
            channel.basicPublish("", this.QUEUE, null, baos.toByteArray());
        } catch (IOException ex) {
            Logger.getLogger(RabbitMQFACConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * METHOD NOT IMPLEMENTED
     * @return EXCEPTION
     * @deprecated Use the callback in the class constructor to read data.
     */
    @Override
    @Deprecated
    public T receive() {
        throw new UnsupportedOperationException("Not supported. Use the constructor callback for receiving messages.");
    }

    @Override
    public void close() throws IOException {
        try {
            channel.close();
            connection.close();
        } catch (TimeoutException ex) {
            throw new IOException(ex);
        }
    }

    public static void main(String[] args) {
        try {
            Properties msgProperties = new Properties();
            msgProperties.load(new FileInputStream("messaging.properties"));
            
            Callback<String, Void> callback = (String param) -> {
                System.out.println(param);
                return null;
            };

            try (IFACConnection<String> conn = new RabbitMQFACConnection<>(
                    msgProperties.getProperty("provider.addr", "127.0.0.1"),
                    Integer.valueOf(msgProperties.getProperty("provider.port", "5672")),
                    msgProperties.getProperty("provider.auth.user", "guest"),
                    msgProperties.getProperty("provider.auth.pass", "guest"),
                    PublicQueues.QUEUE_QUERY, callback)) {
                conn.send("Hello, this is patrick!");
                System.in.read();
            }
        } catch (Exception ex) {
            Logger.getLogger(RabbitMQFACConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
