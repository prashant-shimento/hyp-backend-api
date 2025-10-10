package com.hyp.playground;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import java.util.ArrayList;
import java.util.List;
import org.bson.Document;

public class MongoScript {

    public static void main(String[] args) {
        try (MongoClient mongoClient =
                MongoClients.create("mongodb+srv://apiuser:IggL9Oyj26ehmp83@production.n3ybxpg.mongodb.net")) {
            MongoDatabase database = mongoClient.getDatabase("hyp_backend_db");
            MongoCollection<Document> itemCollection = database.getCollection("items");
            MongoCollection<Document> variationsCollection = database.getCollection("variations");

            // Step 1: Find items with price "0"
            List<Document> itemsToUpdate =
                    itemCollection.find(Filters.eq("price", "0")).into(new ArrayList<>());

            System.out.println("Items to update: " + itemsToUpdate.size());

            for (Document item : itemsToUpdate) {
                Object itemId = item.get("_id");
                System.out.println("Processing item with id: " + itemId);

                // Step 2: Get variations from the item
                List<Object> variationIds = (List<Object>) item.get("variation");
                if (variationIds != null && !variationIds.isEmpty()) {
                    // Step 3: Find the least price from the variations collection
                    List<Document> variations = variationsCollection
                            .find(Filters.in("_id", variationIds))
                            .into(new ArrayList<>());

                    double leastPrice = Double.MAX_VALUE;
                    for (Document variation : variations) {
                        Object priceObj = variation.get("price");
                        if (priceObj != null) {
                            try {
                                double price = Double.parseDouble(priceObj.toString());
                                if (price < leastPrice) {
                                    leastPrice = price;
                                }
                            } catch (NumberFormatException e) {
                                System.err.println("Invalid price format for variation: " + variation.get("_id"));
                            }
                        }
                    }

                    // Step 4: Update the item's price if a valid least price was found
                    if (leastPrice != Double.MAX_VALUE) {
                        itemCollection.updateOne(
                                Filters.eq("_id", itemId), Updates.set("price", String.valueOf(leastPrice)));
                        System.out.println("Updated item with id " + itemId + " to price " + leastPrice);
                    } else {
                        System.out.println("No valid price found for variations of item with id " + itemId);
                    }
                } else {
                    System.out.println("No variations found for item with id " + itemId);
                }
            }

            System.out.println("Item price updates completed successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error during item attribute update: " + e.getMessage());
        }
    }
}
