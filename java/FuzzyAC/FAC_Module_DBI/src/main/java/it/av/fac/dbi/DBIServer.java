/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.dbi;

import it.av.fac.dbi.handlers.DBIHandler;
import it.av.fac.dbi.util.DBIConfig;
import it.av.fac.messaging.client.FACLogger;
import it.av.fac.messaging.interfaces.IFACConnection;
import it.av.fac.messaging.rabbitmq.RabbitMQConnectionWrapper;
import it.av.fac.messaging.rabbitmq.RabbitMQConstants;
import it.av.fac.messaging.rabbitmq.RabbitMQServer;
import it.av.fac.messaging.rabbitmq.test.Server;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Launches the DBI server.
 * @author Diogo Regateiro <diogoregateiro@ua.pt>
 */
public class DBIServer {
    public static void main(String[] args) {
        try (RabbitMQConnectionWrapper connWrapper = RabbitMQConnectionWrapper.getInstance();
                FACLogger logger = new FACLogger(DBIConfig.MODULE_KEY)){
            try (IFACConnection serverConn = new RabbitMQServer(
                    connWrapper, RabbitMQConstants.QUEUE_DBI_REQUEST, new DBIHandler(logger))) {
                System.out.println("DBI Server is now running... enter 'q' to quit.");
                while(System.in.read() != 'q');
            } catch (Exception ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (Exception ex) {
            Logger.getLogger(RabbitMQServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
