/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.dbi.drivers;

import com.mongodb.MongoClient;
import com.mongodb.MongoException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.text;
import static com.mongodb.client.model.Updates.set;
import it.av.fac.dbi.util.DBIConfig;
import it.av.fac.messaging.client.BDFISReply;
import it.av.fac.messaging.client.ReplyStatus;
import it.av.fac.messaging.client.interfaces.IReply;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author Diogo Regateiro <diogoregateiro@ua.pt>
 */
public class DocumentDBI implements Closeable {
    
    private static final String PROVIDER = "document";
    private static final String DATABASE = "fac";
    private static DocumentDBI instance;
    private static Thread autosyncThr;
    
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
        if (instance == null) {
            instance = new DocumentDBI();
            autosyncThr = new Thread(() -> {
                try {
                    while (!Thread.interrupted()) {
                        Thread.sleep(60000);
                        
                        if (System.currentTimeMillis() > instance.lastSync + 60000 && !instance.bulk.isEmpty()) {
                            System.out.println("Autosyncing...");
                            instance.syncWithDB();
                        }
                    }
                } catch (InterruptedException ex) {
                }
                System.out.println("DocumentDBI autosync thread is terminating...");
            });
            autosyncThr.setDaemon(true);
            autosyncThr.start();
        }
        instance.useCollection(collection);
        return instance;
    }
    
    public void useDatabase(String database) {
        if (!this.bulk.isEmpty()) {
            syncWithDB();
        }
        this.mongoDB = this.mongoClient.getDatabase(database);
    }
    
    public void useCollection(String collection) {
        if (!this.bulk.isEmpty()) {
            syncWithDB();
        }
        this.collection = this.mongoDB.getCollection(collection);
    }
    
    public synchronized void syncWithDB() {
        try {
            if (!bulk.isEmpty()) {
                this.collection.insertMany(bulk);
                this.bulk.clear();
                this.lastSync = System.currentTimeMillis();
            }
        } catch (MongoException ex) {
            Logger.getLogger(DocumentDBI.class.getName()).log(Level.SEVERE, null, ex);
            System.err.print("There was an error while trying to sync with mongo, attempting to recover... ");
            this.bulk.stream().map((document) -> {
                return new Document("_id", document.get("_id"));
            }).filter((document) -> {
                return this.collection.count(document) != 0;
            }).forEach((document) -> {
                this.collection.deleteOne(document);
            });
            
            try {
                this.collection.insertMany(bulk);
                this.bulk.clear();
                this.lastSync = System.currentTimeMillis();
                System.err.print("Success!");
            } catch (MongoException ex2) {
                System.err.print("Failure!");
            }
        }
    }
    
    public void storeResource(JSONObject resource) {
        if (this.collection.count(new Document("_id", resource.get("_id"))) == 0) {
            this.bulk.add(new Document(resource.toMap()));
            
            if (this.bulk.size() >= this.bulkSize) {
                System.out.println("Bulk syncing...");
                syncWithDB();
            }
        } else {
            Document filter = new Document("_id", resource.get("_id"));
            resource.remove("_id");
            Document update = new Document("$set", new Document(resource.toMap()));
            this.collection.updateOne(filter, update);
        }
    }
    
    public void updateResource(JSONObject resource, String field) {
        this.collection.updateOne(
                eq("_id", resource.get("_id")),
                set(field, resource.getString(field))
        );
    }

    /**
     * TODO: Add more query functionalities.
     *
     * @param query
     * @return
     */
    public JSONArray query(String query) {
        JSONArray ret = new JSONArray();
        
        FindIterable<Document> documents = this.collection.find(text(query));
        documents.forEach(new Consumer<Document>() {
            @Override
            public void accept(Document doc) {
                ret.put(new JSONObject(doc.toJson()));
            }
        });
        
        return ret;
    }

    /**
     * TODO: Add more query functionalities.
     *
     * @param resourceId
     * @return
     */
    public IReply findResource(Object resourceId) {
        IReply reply = new BDFISReply();
        
        this.collection.find(eq("_id", resourceId)).forEach(new Consumer<Document>() {
            @Override
            public void accept(Document doc) {
                reply.addData(doc.toJson());
            }
        });
        
        return reply;
    }

    /**
     * TODO: Add more query functionalities.
     *
     * @param matchingFields
     * @return
     */
    public IReply findResource(JSONObject matchingFields) {
        IReply reply = new BDFISReply();
        
        this.collection.find(new Document(matchingFields.toMap())).forEach(new Consumer<Document>() {
            @Override
            public void accept(Document doc) {
                reply.addData(doc.toJson());
            }
        });
        
        return reply;
    }
    
    /**
     * TODO: Add more query functionalities.
     *
     * @param id
     * @param sortFields
     * @return
     */
    public IReply findLastResource(Object id, JSONObject sortFields) {
        IReply reply = new BDFISReply();
        
        Document filter = new Document();
        if(id != null) filter.put("userid", id);
        this.collection.find(filter).sort(new Document(sortFields.toMap())).limit(1).forEach(new Consumer<Document>() {
            @Override
            public void accept(Document doc) {
                reply.addData(doc.toJson());
            }
        });
        
        return reply;
    }
    
    public void processResources(Consumer<Document> handler) {
        this.collection.find().forEach(handler);
    }
    
    @Override
    public void close() throws IOException {
        try {
            DocumentDBI.autosyncThr.interrupt();
            DocumentDBI.autosyncThr.join();
        } catch (InterruptedException ex) {
        }
        
        syncWithDB();
        this.mongoClient.close();
        DocumentDBI.autosyncThr = null;
        DocumentDBI.instance = null;
    }
    
    public static void main(String[] args) throws IOException {
        //connection test
        try (DocumentDBI dbi = DocumentDBI.getInstance("wikipages")) {
            System.out.println("Success!");
        }
    }
}
