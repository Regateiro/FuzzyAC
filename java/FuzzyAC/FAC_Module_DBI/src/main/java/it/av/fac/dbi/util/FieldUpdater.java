/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.av.fac.dbi.util;

import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import static com.mongodb.client.model.Filters.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bson.Document;

/**
 *
 * @author Diogo Regateiro <diogoregateiro@ua.pt>
 */
public class FieldUpdater {

    public static void main(String[] args) {
        SimpleDateFormat parser = new SimpleDateFormat("EEE MMM d HH:mm:ss Z yyyy");

        MongoClient mongoClient = new MongoClient("127.0.0.1", 27017);
        MongoDatabase mongoDB = mongoClient.getDatabase("test");
        MongoCollection<Document> collection = mongoDB.getCollection("tweets");

        FindIterable<Document> documents = collection.find();
        documents.forEach(new Consumer<Document>() {
            @Override
            public void accept(Document doc) {
                try {
                    //System.out.println(doc.toJson());
                    Document user = (Document) doc.get("user");
                    String dateStr = user.getString("created_at");
                    //System.out.println(dateStr);
                    Date date = parser.parse(dateStr);
                    //System.out.println(date);
                    System.out.println(
                            collection.updateOne(eq("_id", doc.get("_id")), new Document("$set", new Document("user.created_at", date)))
                    );
                } catch (ParseException ex) {
                    Logger.getLogger(FieldUpdater.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
    }
}
