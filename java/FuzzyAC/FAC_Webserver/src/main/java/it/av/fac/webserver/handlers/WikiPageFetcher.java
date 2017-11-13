/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.webserver.handlers;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.bson.Document;
import org.bson.conversions.Bson;

/**
 *
 * @author Diogo Regateiro
 */
public class WikiPageFetcher implements Closeable {

    private static final String DATABASE = "fac";
    private static final String COLLECTION = "metadata";
    private static WikiPageFetcher instance;
    private final MongoClient mongoClient;
    private final MongoDatabase mongoDB;
    private final MongoCollection<Document> collection;

    private WikiPageFetcher(String dbAddr, int dbPort) throws IOException {
        //add your option to the connection 
        this.mongoClient = new MongoClient(dbAddr, dbPort);
        this.mongoDB = this.mongoClient.getDatabase(WikiPageFetcher.DATABASE);
        this.collection = this.mongoDB.getCollection(WikiPageFetcher.COLLECTION);
    }

    public static WikiPageFetcher getInstance(String dbAddr, int dbPort) throws IOException {
        if (instance == null) {
            instance = new WikiPageFetcher(dbAddr, dbPort);
        }
        return instance;
    }

    /**
     * TODO: Add more query functionalities.
     *
     * @param query
     * @return
     */
    public JSONArray search(String query) {
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
     * @param page
     * @return
     */
    public JSONArray fetchPage(String page) {
        JSONArray ret = new JSONArray();

        List<Bson> filters = new ArrayList<>();
        filters.add(Filters.eq("_id", page));

        FindIterable<Document> documents = this.collection.find(Filters.and(filters));

        documents.forEach(new Consumer<Document>() {
            @Override
            public void accept(Document doc) {
                ret.add(JSONObject.parseObject(doc.toJson()));
            }
        });
        return ret;
    }

    @Override
    public void close() throws IOException {
        this.mongoClient.close();
    }
}
