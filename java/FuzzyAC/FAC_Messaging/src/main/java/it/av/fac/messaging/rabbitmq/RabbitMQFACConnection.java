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
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
    private final String QUEUE_IN;
    private final String QUEUE_OUT;

    /**
     *
     * @param addr
     * @param port
     * @param username
     * @param password
     * @param queueOut
     * @param queueIn
     * @param callback
     * @throws Exception
     */
    public RabbitMQFACConnection(String addr, int port, String username, String password, String queueOut, String queueIn, Callback<T, Void> callback) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(addr);
        factory.setPort(port);
        factory.setUsername(username);
        factory.setPassword(password);

        this.QUEUE_IN = queueIn;
        this.QUEUE_OUT = queueOut;

        this.connection = factory.newConnection();
        this.channel = connection.createChannel();
        this.channel.exchangeDeclare(RabbitMQInternalConstants.EXCHANGE, "direct", true);
        this.channel.queueDeclare(this.QUEUE_IN, true, false, false, null);
        this.channel.queueDeclare(this.QUEUE_OUT, true, false, false, null);
        this.channel.queueBind(this.QUEUE_IN, RabbitMQInternalConstants.EXCHANGE, this.QUEUE_IN);
        this.channel.queueBind(this.QUEUE_OUT, RabbitMQInternalConstants.EXCHANGE, this.QUEUE_OUT);
        
        this.channel.basicConsume(this.QUEUE_IN, true, new DefaultConsumer(this.channel) {
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

    @Override
    public void send(T message) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(message);
            this.channel.basicPublish(RabbitMQInternalConstants.EXCHANGE, this.QUEUE_OUT, null, baos.toByteArray());
        } catch (IOException ex) {
            Logger.getLogger(RabbitMQFACConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * METHOD NOT IMPLEMENTED
     *
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
}
