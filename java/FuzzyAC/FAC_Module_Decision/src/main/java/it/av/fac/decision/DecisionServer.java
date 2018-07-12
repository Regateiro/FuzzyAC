/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.decision;

import it.av.fac.decision.handlers.DecisionHandler;
import it.av.fac.decision.util.decision.DecisionConfig;
import it.av.fac.messaging.client.FACLogger;
import it.av.fac.messaging.interfaces.IFACConnection;
import it.av.fac.messaging.rabbitmq.RabbitMQConnectionWrapper;
import it.av.fac.messaging.rabbitmq.RabbitMQConstants;
import it.av.fac.messaging.rabbitmq.RabbitMQServer;
import it.av.fac.messaging.rabbitmq.test.Server;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Launches the decision server.
 * @author Diogo Regateiro <diogoregateiro@ua.pt>
 */
public class DecisionServer {

    public static void main(String[] args) {
        try (RabbitMQConnectionWrapper connWrapper = RabbitMQConnectionWrapper.getInstance();
                FACLogger logger = new FACLogger(DecisionConfig.MODULE_KEY)) {
            try (IFACConnection serverConn = new RabbitMQServer(
                    connWrapper, RabbitMQConstants.QUEUE_DECISION_REQUEST, new DecisionHandler(logger))) {
                System.out.println("Enforcement Server is now running... enter 'q' to quit.");
                while (System.in.read() != 'q');
            } catch (Exception ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (Exception ex) {
            Logger.getLogger(RabbitMQServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
