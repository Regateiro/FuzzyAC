/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.messaging.rabbitmq;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import it.av.fac.messaging.interfaces.IFACConnection;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import it.av.fac.messaging.interfaces.IServerHandler;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

/**
 * Allows to send and receive messages within the FAC architecture.
 *
 * @author Diogo Regateiro
 */
public class RabbitMQServer implements IFACConnection {

    private final Connection conn;
    private final Channel channel;
    private final String queueIn;
    private final String queueOut;
    private final String routingKeyIn;
    private final String routingKeyOut;
    private final boolean canSend;

    /**
     * Instantiates a RabbitMQ connection to send a reply only. Attempts to
     * recover a previously used channel.
     *
     * @param connWrapper The object that contains a connection to the RabbitMQ
     * server.
     * @param queueOut The queue where this connection should send messages to.
     * @param routingKey
     * @throws Exception If there is a connection issue to the RabbitMQ server.
     */
    public RabbitMQServer(RabbitMQConnectionWrapper connWrapper, String queueOut, String routingKey) throws Exception {
        this.queueIn = null;
        this.queueOut = queueOut + "." + routingKey;
        this.routingKeyIn = null;
        this.routingKeyOut = this.queueOut;
        this.canSend = true;

        this.conn = connWrapper.getConnection();
        this.channel = RabbitMQChannelPool.createChannel(conn, this.queueIn, this.queueOut, this.routingKeyIn, this.routingKeyOut);
    }

    /**
     * Instantiates a RabbitMQ connection to receive a request only. Cannot send
     * messages.
     *
     * @param connWrapper The object that contains a connection to the RabbitMQ
     * server.
     * @param queueIn The queue where this connection should read messages from.
     * @param handler The handler to call when new messages are available in the
     * queueIn. Replaces the receive method.
     * @throws Exception If there is a connection issue to the RabbitMQ server.
     */
    public RabbitMQServer(RabbitMQConnectionWrapper connWrapper, String queueIn, IServerHandler<String, String> handler) throws Exception {
        this.queueIn = queueIn;
        this.queueOut = null;
        this.routingKeyIn = queueIn + ".#";
        this.routingKeyOut = null;
        this.canSend = false;

        this.conn = connWrapper.getConnection();
        this.channel = RabbitMQChannelPool.createChannel(conn, this.queueIn, this.queueOut, this.routingKeyIn, this.routingKeyOut);

        this.channel.basicConsume(this.queueIn, true, new DefaultConsumer(this.channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
                    throws IOException {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(body)))) {
                    String clientKey = envelope.getRoutingKey().substring(queueIn.length() + 1);
                    handler.handle(in.readLine(), clientKey);
                } catch (Exception ex) {
                    Logger.getLogger(RabbitMQServer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        });
    }

    @Override
    public void send(String message) {
        if (canSend) {
            try {
                this.channel.basicPublish(RabbitMQInternalConstants.EXCHANGE, this.routingKeyOut, null, message.getBytes(Charset.forName("UTF-8")));
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
    public String receive() {
        throw new UnsupportedOperationException("Not supported. Use the constructor callback for receiving messages.");
    }

    @Override
    public void close() throws IOException {
        RabbitMQChannelPool.release(this.queueIn, this.queueOut, this.routingKeyIn, this.routingKeyOut);
    }
}
