package com.hyp.playground;

import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

public class RevertItemPriceUpdate {
    public static void main(String[] args) {
        try (MongoClient mongoClient = MongoClients.create("mongodb+srv://apiuser:IggL9Oyj26ehmp83@production.n3ybxpg.mongodb.net")) {
            MongoDatabase database = mongoClient.getDatabase("hyp_backend_db");
            MongoCollection<Document> itemCollection = database.getCollection("items");

            // Step 1: Find items where item_allow_variation is 1 and variation array is not empty
            List<Document> itemsToRevert = itemCollection.find(
                    Filters.and(
                            Filters.eq("item_allow_variation", "1"),
                            Filters.ne("variation", new ArrayList<>()), // Ensures variation array is not empty
                            Filters.exists("variation", true)          // Ensures variation field exists
                    )
            ).into(new ArrayList<>());

            System.out.println("Items to revert: " + itemsToRevert.size());

            for (Document item : itemsToRevert) {
                Object itemId = item.get("_id");
                System.out.println("Reverting price for item with id: " + itemId);

                // Step 2: Update item's price to "0"
                itemCollection.updateOne(
                        Filters.eq("_id", itemId),
                        Updates.set("price", "0")
                );

                System.out.println("Reverted item with id " + itemId + " price to 0");
            }

            System.out.println("Item price reversion completed successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error during item price reversion: " + e.getMessage());
        }
    }
}
