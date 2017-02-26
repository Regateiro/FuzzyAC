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
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Stores channels for reuse.
 *
 * @author Diogo Regateiro
 */
public class RabbitMQChannelPool {

    private final static Map<String, ChannelInfo> POOL = new HashMap<>();
    private final static ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(1);

    public static synchronized Channel createChannel(Connection conn, String queueIn, String queueOut, String routingKeyIn, String routingKeyOut) throws IOException {
        String UID = obtainUniqueId(queueIn, queueOut, routingKeyIn, routingKeyOut);

        Channel channel;
        if (!POOL.containsKey(UID)) {
            channel = conn.createChannel();
            channel.exchangeDeclare(RabbitMQInternalConstants.EXCHANGE, "direct", true);
            if (queueIn != null) {
                channel.queueDeclare(queueIn, false, false, true, null);
                channel.queueBind(queueIn, RabbitMQInternalConstants.EXCHANGE, routingKeyIn);
            }
            if (queueOut != null) {
                channel.queueDeclare(queueOut, false, false, true, null);
                channel.queueBind(queueOut, RabbitMQInternalConstants.EXCHANGE, routingKeyOut);
            }
            if (POOL.isEmpty()) {
                SCHEDULER.schedule(CLEANER, 5, TimeUnit.MINUTES);
            }
            POOL.put(UID, new ChannelInfo(channel));
        } else {
            ChannelInfo channelInfo = POOL.get(UID);
            channelInfo.unrelease();
            channel = channelInfo.getChannel();
        }

        return channel;
    }

    private static String obtainUniqueId(String queueIn, String queueOut, String routingKeyIn, String routingKeyOut) {
        return String.format("%s|%s|%s|%s", queueIn, queueOut, routingKeyIn, routingKeyOut);
    }

    public static synchronized void release(String queueIn, String queueOut, String routingKeyIn, String routingKeyOut) {
        String UID = obtainUniqueId(queueIn, queueOut, routingKeyIn, routingKeyOut);

        if (POOL.containsKey(UID)) {
            POOL.get(UID).release();
        }
    }

    private static synchronized void removeChannel(String UID) {
        POOL.remove(UID);
    }

    private static class ChannelInfo {

        private final Channel channel;
        private boolean isReleased;
        private long releaseTimestamp;

        public ChannelInfo(Channel channel) {
            this.channel = channel;
            this.isReleased = false;
            this.releaseTimestamp = -1;
        }

        public Channel getChannel() {
            return channel;
        }

        public void release() {
            this.isReleased = true;
            this.releaseTimestamp = System.currentTimeMillis();
        }

        public void unrelease() {
            this.isReleased = false;
            this.releaseTimestamp = -1;
        }

        public boolean isFitForRemoval() {
            return isReleased && (System.currentTimeMillis() > (this.releaseTimestamp + 300000));
        }
    }

    private static final Runnable CLEANER = new Runnable() {
        @Override
        public void run() {
            Set<String> UIDs = POOL.keySet();
            UIDs.parallelStream().forEach((UID) -> {
                ChannelInfo cinfo = POOL.get(UID);
                if (cinfo.isFitForRemoval()) {
                    removeChannel(UID);
                }
            });

            if (!POOL.isEmpty()) {
                SCHEDULER.schedule(this, 5, TimeUnit.MINUTES);
            }
        }
    };
}
