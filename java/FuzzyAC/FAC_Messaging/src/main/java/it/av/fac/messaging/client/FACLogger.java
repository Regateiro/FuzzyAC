/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.messaging.client;

import it.av.fac.messaging.rabbitmq.RabbitMQClient;
import it.av.fac.messaging.rabbitmq.RabbitMQConnectionWrapper;
import it.av.fac.messaging.rabbitmq.RabbitMQConstants;
import java.io.Closeable;
import java.io.IOException;

/**
 *
 * @author Regateiro
 */
public class FACLogger implements Closeable {
    
    private final RabbitMQClient conn;
    private final String module_key;

    public FACLogger(String module_key) throws Exception {
        this.module_key = module_key;
        this.conn = new RabbitMQClient(RabbitMQConnectionWrapper.getInstance(),
                RabbitMQConstants.QUEUE_MONITOR_RESPONSE,
                RabbitMQConstants.QUEUE_MONITOR_REQUEST, module_key, (byte[] message) -> {});
    }
    
    public void info(String msg) throws IOException {
        BDFISRequest request = new BDFISRequest(module_key, msg, RequestType.LogInfo);
        conn.send(request.convertToBytes());
    }
    
    public void warning(String msg) throws IOException {
        BDFISRequest request = new BDFISRequest(module_key, msg, RequestType.LogWarning);
        conn.send(request.convertToBytes());
    }
    
    public void error(String msg) throws IOException {
        BDFISRequest request = new BDFISRequest(module_key, msg, RequestType.LogError);
        conn.send(request.convertToBytes());
    }

    @Override
    public void close() throws IOException {
        this.conn.close();
    }
}
