/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.wikipedia.client.util;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.result.UpdateResult;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.bson.Document;
import org.json.JSONObject;

/**
 *
 * @author DiogoJosé
 */
public class MongoDBStorage implements IStorage<JSONObject> {

    private final MongoClient mongoClient;
    private final MongoDatabase mongoDB;
    private final MongoCollection<Document> mongoCol;

    public MongoDBStorage(String addr, int port, String database, String collection) {
        this.mongoClient = new MongoClient(addr, port);
        this.mongoDB = this.mongoClient.getDatabase(database);
        this.mongoCol = this.mongoDB.getCollection(collection);
    }

    @Override
    public boolean insert(JSONObject data) {
        this.mongoCol.insertOne(new Document(data.toMap()));
        return true;
    }

    @Override
    public boolean update(JSONObject id, JSONObject updatedFields) {
        return this.mongoCol.updateOne(new Document(id.toMap()), new Document("$set", new Document(updatedFields.toMap()))).getMatchedCount() == 1;
    }

    @Override
    public boolean delete(JSONObject matchingData) {
        return this.mongoCol.deleteOne(new Document(matchingData.toMap())).getDeletedCount() > 0;
    }

    @Override
    public List<JSONObject> select(JSONObject matchingData) {
        List<JSONObject> ret = new ArrayList<>();
        this.mongoCol.find(new Document(matchingData.toMap())).forEach(new Consumer<Document>() {
            @Override
            public void accept(Document t) {
                ret.add(new JSONObject(t));
            }
        });
        return ret;
    }

    @Override
    public void index(boolean unique, String ... keys) {
        IndexOptions opt = new IndexOptions();
        opt.unique(unique);
        Document indexBson = new Document();
        for(String key : keys) {
            indexBson.append(key, 1);
        }
        System.out.println(this.mongoCol.createIndex(indexBson, opt));
    }
}
