/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.messaging.rabbitmq;

import java.io.Serializable;
import it.av.fac.messaging.interfaces.client.IFACConnection;

/**
 * Allows to send and receive messages within the FAC architecture.
 * @author Diogo Regateiro
 * @param <T> The type of the message to send/receive.
 */
public class RabbitMQFACConnection<T extends Serializable> implements IFACConnection<T> {

    /**
     * Initializes the FACConnection object.
     */
    public RabbitMQFACConnection() {
    }
    
    @Override
    public void send(T message) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public T receive() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
