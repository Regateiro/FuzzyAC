/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.information;

import it.av.fac.information.handlers.InformationHandler;
import it.av.fac.messaging.interfaces.IFACConnection;
import it.av.fac.messaging.rabbitmq.RabbitMQConnectionWrapper;
import it.av.fac.messaging.rabbitmq.RabbitMQConstants;
import it.av.fac.messaging.rabbitmq.RabbitMQServer;
import it.av.fac.messaging.rabbitmq.test.Server;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Launches the decision server.
 * @author Diogo Regateiro <diogoregateiro@ua.pt>
 */
public class InformationServer {

    @SuppressWarnings("empty-statement")
    public static void main(String[] args) {
        try (RabbitMQConnectionWrapper connWrapper = RabbitMQConnectionWrapper.getInstance()) {
            try (IFACConnection serverConn = new RabbitMQServer(
                    connWrapper, RabbitMQConstants.QUEUE_INFORMATION_REQUEST, new InformationHandler())) {
                System.out.println("Enforcement Server is now running... enter 'q' to quit.");
                while (System.in.read() != 'q');
            } catch (Exception ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (NumberFormatException | IOException | TimeoutException ex) {
            Logger.getLogger(RabbitMQServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
