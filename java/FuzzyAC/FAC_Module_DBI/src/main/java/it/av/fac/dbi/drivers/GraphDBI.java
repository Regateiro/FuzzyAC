/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.dbi.drivers;

import it.av.fac.messaging.client.DBIRequest;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.Statement;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 *
 * @author Diogo Regateiro
 */
public class GraphDBI implements Closeable {

    private static final String CQL_CREATE_SIMPLE_NODE = "CREATE (n:%s)";
    private static final String CQL_CREATE_RELATIONSHIP = "MATCH (o:%s),(d:%s) CREATE (o)-[r:%s]->(d)";
    private static final String PROVIDER = "graph";
    private final Driver driver;
    private final Session session;

    public GraphDBI() throws IOException {
        Properties msgProperties = new Properties();
        msgProperties.load(new FileInputStream("dbi.properties"));

        String addr = msgProperties.getProperty(PROVIDER + ".addr", "127.0.0.1");
        int port = Integer.valueOf(msgProperties.getProperty(PROVIDER + ".port", "7687"));
        String username = msgProperties.getProperty(PROVIDER + ".auth.user", "neo4j");
        String password = msgProperties.getProperty(PROVIDER + ".auth.pass", "neo4j");
        this.driver = GraphDatabase.driver(String.format("bolt://%s:%d", addr, port), AuthTokens.basic(username, password));
        this.session = this.driver.session();
    }

    /**
     * Creates a node with the given properties.
     * @param label
     * @param props 
     */
    private void createNode(String label, Map<String, String> props) {
        StringBuilder strb = new StringBuilder(CQL_CREATE_SIMPLE_NODE);
        if (props != null && !props.isEmpty()) {
            strb.append("{");
            
            props.keySet().forEach((key) -> {
                strb.append(key).append(":").append(props.get(key)).append(",");
            });
            
            strb.deleteCharAt(strb.length() - 1);
            strb.append("}");
        }
        this.session.run(new Statement(String.format(strb.toString(), label)));
    }
    
    /**
     * TODO
     * @param label
     * @return 
     */
    private Record matchNode(String label) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    private void createRelationship(String originNodeLabel, String relationName, String destinationNodeLabel) {
        this.session.run(new Statement(String.format(CQL_CREATE_RELATIONSHIP, originNodeLabel, destinationNodeLabel, relationName)));
    }

    @Override
    public void close() throws IOException {
        this.driver.close();
    }

    public void storeNode(DBIRequest request) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
