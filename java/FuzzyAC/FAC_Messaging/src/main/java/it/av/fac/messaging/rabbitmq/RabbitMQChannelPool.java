/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.messaging.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Stores channels for reuse.
 *
 * @author Diogo Regateiro
 */
public class RabbitMQChannelPool {

    private final static Map<String, Channel> POOL = new HashMap<>();

    public static synchronized Channel createChannel(Connection conn, String queueIn, String queueOut, String routingKeyIn, String routingKeyOut) throws IOException {
        String UID = obtainUniqueId(queueIn, queueOut, routingKeyIn, routingKeyOut);

        if (!POOL.containsKey(UID)) {
            Channel channel = conn.createChannel();
            channel.exchangeDeclare(RabbitMQConstants.EXCHANGE, "direct", true);
            if (queueIn != null) {
                channel.queueDeclare(queueIn, false, false, true, null);
                channel.queueBind(queueIn, RabbitMQConstants.EXCHANGE, routingKeyIn);
            }
            if (queueOut != null) {
                channel.queueDeclare(queueOut, false, false, true, null);
                channel.queueBind(queueOut, RabbitMQConstants.EXCHANGE, routingKeyOut);
            }

            POOL.put(UID, channel);
        }

        return POOL.get(UID);
    }

    private static String obtainUniqueId(String queueIn, String queueOut, String routingKeyIn, String routingKeyOut) {
        return String.format("%s|%s|%s|%s", queueIn, queueOut, routingKeyIn, routingKeyOut);
    }
}
