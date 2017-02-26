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
import it.av.fac.messaging.interfaces.IServerHandler;

/**
 * Allows to send and receive messages within the FAC architecture.
 *
 * @author Diogo Regateiro
 * @param <S> The type of the message to send.
 * @param <R> The type of the message to receive.
 */
public class RabbitMQServer<S extends Serializable, R extends Serializable> implements IFACConnection<S, R> {

    private final Connection conn;
    private final Channel channel;
    private final String queueIn;
    private final String queueOut;
    private final boolean canSend;

    /**
     * Instantiates a RabbitMQ connection to send a reply only.
     *
     * @param addr The address of the RabbitMQ server.
     * @param port The port where the RabbitMQ server is running.
     * @param username The username to connect to the RabbitMQ server.
     * @param password The password to connect to the RabbitMQ server.
     * @param queueOut The queue where this connection should send messages to.
     * @param routingKey
     * @throws Exception If there is a connection issue to the RabbitMQ server.
     */
    public RabbitMQServer(String addr, int port, String username, String password, String queueOut, String routingKey) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(addr);
        factory.setPort(port);
        factory.setUsername(username);
        factory.setPassword(password);

        this.queueIn = null;
        this.queueOut = queueOut + "." + routingKey;
        this.canSend = true;

        this.conn = factory.newConnection();
        this.channel = conn.createChannel();
        this.channel.exchangeDeclare(RabbitMQInternalConstants.EXCHANGE, "direct", true);
        this.channel.queueDeclare(this.queueOut, false, false, true, null);
        this.channel.queueBind(this.queueOut, RabbitMQInternalConstants.EXCHANGE, this.queueOut);
    }

    /**
     * Instantiates a RabbitMQ connection to receive a request only. Cannot send messages.
     *
     * @param addr The address of the RabbitMQ server.
     * @param port The port where the RabbitMQ server is running.
     * @param username The username to connect to the RabbitMQ server.
     * @param password The password to connect to the RabbitMQ server.
     * @param queueIn The queue where this connection should read messages from.
     * @param handler The handler to call when new messages are available in the
     * queueIn. Replaces the receive method.
     * @throws Exception If there is a connection issue to the RabbitMQ server.
     */
    public RabbitMQServer(String addr, int port, String username, String password, String queueIn, IServerHandler<R, String> handler) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(addr);
        factory.setPort(port);
        factory.setUsername(username);
        factory.setPassword(password);

        this.queueIn = queueIn;
        this.queueOut = null;
        this.canSend = false;

        this.conn = factory.newConnection();
        this.channel = conn.createChannel();
        this.channel.exchangeDeclare(RabbitMQInternalConstants.EXCHANGE, "direct", true);
        this.channel.queueDeclare(this.queueIn, false, false, true, null);
        this.channel.queueBind(this.queueIn, RabbitMQInternalConstants.EXCHANGE, this.queueIn + ".#");

        this.channel.basicConsume(this.queueIn, true, new DefaultConsumer(this.channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
                    throws IOException {
                try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(body))) {
                    String clientKey = envelope.getRoutingKey().substring(queueIn.length() + 1);
                    handler.handle((R) ois.readObject(), clientKey);
                } catch (Exception ex) {
                    Logger.getLogger(RabbitMQServer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        });
    }

    @Override
    public void send(S message) {
        if (canSend) {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(baos)) {
                oos.writeObject(message);
                this.channel.basicPublish(RabbitMQInternalConstants.EXCHANGE, this.queueOut, null, baos.toByteArray());
            } catch (IOException ex) {
                Logger.getLogger(RabbitMQServer.class.getName()).log(Level.SEVERE, null, ex);
            }
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
    public R receive() {
        throw new UnsupportedOperationException("Not supported. Use the constructor callback for receiving messages.");
    }

    @Override
    public void close() throws IOException {
        try {
            channel.close();
            conn.close();
        } catch (TimeoutException ex) {
            throw new IOException(ex);
        }
    }
}
