/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.dbi.drivers;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import it.av.fac.dbi.handlers.DBIConfig;
import it.av.fac.messaging.client.StorageRequest;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.bson.Document;

/**
 *
 * @author Diogo Regateiro
 */
public class DocumentDBI implements Closeable {

    private static final String PROVIDER = "document";
    private static final String DATABASE = "fac";
    private static DocumentDBI instance;

    private final List<Document> bulk;
    private final MongoClient mongoClient;
    private final int bulkSize = 1000;
    private long lastSync;
    private MongoDatabase mongoDB;
    private MongoCollection<Document> collection;

    private DocumentDBI() throws IOException {
        Properties msgProperties = new Properties();
        msgProperties.load(new FileInputStream(DBIConfig.PROPERTIES_FILE));

        String addr = msgProperties.getProperty(PROVIDER + ".addr", "127.0.0.1");
        int port = Integer.valueOf(msgProperties.getProperty(PROVIDER + ".port", "27017"));
        String username = msgProperties.getProperty(PROVIDER + ".auth.user", "mongo");
        String password = msgProperties.getProperty(PROVIDER + ".auth.pass", "mongo");

        //add your option to the connection 
        this.mongoClient = new MongoClient(addr, port);
        this.mongoDB = this.mongoClient.getDatabase(DocumentDBI.DATABASE);
        this.bulk = new ArrayList<>(this.bulkSize);
        this.lastSync = System.currentTimeMillis();
    }
    
    public static DocumentDBI getInstance(String collection) throws IOException {
        if(instance == null) {
            instance = new DocumentDBI();
            
            Thread t = new Thread(() -> {
                while(true) {
                    try {
                        Thread.sleep(60000);
                    } catch (InterruptedException ex) {
                    }
                    if(System.currentTimeMillis() > instance.lastSync + 60000 && !instance.bulk.isEmpty()) {
                        System.out.println("Autosyncing...");
                        instance.syncWithDB();
                    }
                }
            });
            t.setDaemon(true);
            t.start();
        }
        instance.useCollection(collection);
        return instance;
    }

    public void useDatabase(String database) {
        this.mongoDB = this.mongoClient.getDatabase(database);
    }

    public void useCollection(String collection) {
        this.collection = this.mongoDB.getCollection(collection);
    }

    public void syncWithDB() {
        this.lastSync = System.currentTimeMillis();
        this.collection.insertMany(bulk);
        this.bulk.clear();
    }

    public void storeDocument(StorageRequest request) {
        Document doc = new Document();
        doc.append("document", request.getDocument());
        doc.putAll(request.getAditionalInfo());
        this.bulk.add(doc);
        
        if (this.bulk.size() == this.bulkSize) {
            System.out.println("Bulk syncing...");
            syncWithDB();
        }
    }

    @Override
    public void close() throws IOException {
        syncWithDB();
        this.mongoClient.close();
    }

    public static void main(String[] args) throws IOException {
        //connection test
        try (DocumentDBI dbi = DocumentDBI.getInstance("wikipages")) {
            System.out.println("Success!");
        }
    }
}
