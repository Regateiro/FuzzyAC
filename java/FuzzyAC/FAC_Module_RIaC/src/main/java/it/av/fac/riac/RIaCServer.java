/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.riac;

import it.av.fac.messaging.client.FACLogger;
import it.av.fac.messaging.interfaces.IFACConnection;
import it.av.fac.messaging.rabbitmq.RabbitMQConnectionWrapper;
import it.av.fac.messaging.rabbitmq.RabbitMQConstants;
import it.av.fac.messaging.rabbitmq.RabbitMQServer;
import it.av.fac.messaging.rabbitmq.test.Server;
import it.av.fac.riac.classifier.HeadingClassifier;
import it.av.fac.riac.handlers.RIaCConfig;
import it.av.fac.riac.handlers.RIaCHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Regateiro
 */
public class RIaCServer {
    public static void main(String[] args) {
        try (RabbitMQConnectionWrapper connWrapper = RabbitMQConnectionWrapper.getInstance();
                FACLogger logger = new FACLogger(RIaCConfig.MODULE_KEY)){
            try (IFACConnection serverConn = new RabbitMQServer(
                    connWrapper, RabbitMQConstants.QUEUE_RIAC_REQUEST, new RIaCHandler(new HeadingClassifier(), logger))) {
                System.out.println("RIaC Server is now running... enter 'q' to quit.");
                while(System.in.read() != 'q');
            } catch (Exception ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (Exception ex) {
            Logger.getLogger(RabbitMQServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
