/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.dbi.drivers;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import it.av.fac.dbi.util.DBIConfig;
import it.av.fac.messaging.client.BDFISReply;
import it.av.fac.messaging.client.interfaces.IReply;
import it.av.fac.messaging.client.interfaces.IRequest;
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
import org.bson.conversions.Bson;

/**
 *
 * @author Diogo Regateiro
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
        this.mongoDB = this.mongoClient.getDatabase(database);
    }

    public void useCollection(String collection) {
        this.collection = this.mongoDB.getCollection(collection);
    }

    public synchronized void syncWithDB() {
        try {
            this.lastSync = System.currentTimeMillis();
            if (!bulk.isEmpty()) {
                this.collection.insertMany(bulk);
                this.bulk.clear();
            }
        } catch (MongoException ex) {
            System.err.println("There was an error while trying to sync with mongo, please check the database.");
            Logger.getLogger(DocumentDBI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void storeResource(JSONObject resource) {
        Document doc = new Document();
        doc.putAll(resource);
        this.bulk.add(doc);

        if (this.bulk.size() == this.bulkSize) {
            System.out.println("Bulk syncing...");
            syncWithDB();
        }
    }

    /**
     * TODO: Add more query functionalities.
     *
     * @param query
     * @return
     */
    public JSONArray query(String query) {
        JSONArray ret = new JSONArray();

        FindIterable<Document> documents = this.collection.find(Filters.text(query));
        documents.forEach(new Consumer<Document>() {
            @Override
            public void accept(Document doc) {
                ret.add(JSONObject.parse(doc.toJson()));
            }
        });

        return ret;
    }

    /**
     * TODO: Add more query functionalities.
     *
     * @param request
     * @return
     */
    public IReply findPolicy(IRequest request) {
        IReply reply = new BDFISReply();

        JSONObject fields = JSONObject.parseObject((String) request.getResourceId());
        List<Bson> filters = new ArrayList<>();
        fields.keySet().stream().forEach((field) -> {
            filters.add(Filters.eq(field, fields.getString(field)));
        });

        FindIterable<Document> documents;
        if (fields.isEmpty()) {
            documents = this.collection.find();
        } else {
            documents = this.collection.find(Filters.and(filters));
        }

        documents.forEach(new Consumer<Document>() {
            @Override
            public void accept(Document doc) {
                reply.addData(doc.toJson());
            }
        });
        return reply;
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
