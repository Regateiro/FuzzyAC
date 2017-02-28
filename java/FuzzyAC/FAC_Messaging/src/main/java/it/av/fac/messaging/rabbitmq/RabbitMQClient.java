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
import it.av.fac.messaging.interfaces.IClientHandler;
import it.av.fac.messaging.interfaces.IFACConnection;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Allows to send and receive messages within the FAC architecture.
 *
 * @author Diogo Regateiro
 */
public class RabbitMQClient implements IFACConnection {

    private final Connection conn;
    private final Channel channel;
    private final String queueIn;
    private final String queueOut;
    private final String routingKeyIn;
    private final String routingKeyOut;

    /**
     * Instantiates a RabbitMQ connection and binds it to two queues.
     *
     * @param connWrapper The object that contains a connection to the RabbitMQ
     * server.
     * @param queueOut The queue where this connection should send messages to.
     * @param queueIn The queue where this connection should read messages from.
     * @param clientKey The unique client key to be used for routing messages.
     * @param handler The handler to call when new messages are available in the
     * queueIn. Replaces the receive method.
     * @throws Exception If there is a connection issue to the RabbitMQ server.
     */
    public RabbitMQClient(RabbitMQConnectionWrapper connWrapper, String queueIn, String queueOut, String clientKey, IClientHandler<byte[]> handler) throws Exception {
        this.queueIn = queueIn + "." + clientKey;
        this.queueOut = queueOut;
        this.routingKeyIn = this.queueIn;
        this.routingKeyOut = this.queueOut + "." + clientKey;

        this.conn = connWrapper.getConnection();
        this.channel = RabbitMQChannelPool.createChannel(conn, this.queueIn, this.queueOut, this.routingKeyIn, this.routingKeyOut);

        this.channel.basicConsume(this.queueIn, true, new DefaultConsumer(this.channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
                    throws IOException {
                handler.handle(body);
            }
        });
    }

    @Override
    public void send(byte[] message) {
        try {
            this.channel.basicPublish(RabbitMQInternalConstants.EXCHANGE, this.routingKeyOut, null, message);
        } catch (IOException ex) {
            Logger.getLogger(RabbitMQServer.class.getName()).log(Level.SEVERE, null, ex);
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
    public byte[] receive() {
        throw new UnsupportedOperationException("Not supported. Use the constructor callback for receiving messages.");
    }

    @Override
    public void close() throws IOException {
        RabbitMQChannelPool.release(this.queueIn, this.queueOut, this.routingKeyIn, this.routingKeyOut);
    }
}
