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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    private final java.util.Queue<T> receiveBuffer;

    /**
     * Initializes the FACConnection object.
     *
     * Important Properties: "provider.queue" - REQUIRED - String with the name
     * of the queue to bind the connection to. "provider.consumer" - REQUIRED -
     * Boolean that indicates whether this connection should consume from the
     * queue or just produce.
     *
     * @param msgProps The properties necessary to establish a connection.
     * @throws java.io.IOException If the connection to the rabbitmq server
     * fails.
     */
    public RabbitMQFACConnection(Properties msgProps) throws Exception {
        if (Boolean.parseBoolean(msgProps.getProperty("messaging", "true"))) {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(msgProps.getProperty("provider.addr", "127.0.0.1"));
            factory.setPort(Integer.valueOf(msgProps.getProperty("provider.port", "5672")));
            factory.setUsername(msgProps.getProperty("provider.auth.user", "guest"));
            factory.setPassword(msgProps.getProperty("provider.auth.pass", "guest"));

            QUEUE = msgProps.getProperty("provider.queue");
            receiveBuffer = new ConcurrentLinkedQueue<>();

            connection = factory.newConnection();
            channel = connection.createChannel();
            channel.queueDeclare(QUEUE, false, false, false, null);

            if (Boolean.parseBoolean(msgProps.getProperty("provider.consumer", "true"))) {
                channel.basicConsume(QUEUE, true, new DefaultConsumer(channel) {
                    @Override
                    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
                            throws IOException {
                        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(body))) {
                            receiveBuffer.add((T) ois.readObject());
                        } catch (Exception ex) {
                            Logger.getLogger(RabbitMQFACConnection.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                });
            }
        } else {
            throw new Exception("Messaging is disabled and an FACConnection requiring messaging was attempted to be instantiated.");
        }
    }

    @Override
    public void send(T message) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(message);
            channel.basicPublish("", QUEUE, null, baos.toByteArray());
        } catch (IOException ex) {
            Logger.getLogger(RabbitMQFACConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public T receive() {
        return receiveBuffer.poll();
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
            msgProperties.put("provider.queue", PublicQueues.QUEUE_QUERY);
            msgProperties.put("provider.consumer", true);

            try (IFACConnection<String> conn = new RabbitMQFACConnection<>(msgProperties)) {
                conn.send("Hello, this is patrick!");

                String reply = null;
                while ((reply = conn.receive()) == null) {
                    Thread.sleep(1000);
                }
                System.out.println(reply);
            }
        } catch (Exception ex) {
            Logger.getLogger(RabbitMQFACConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
