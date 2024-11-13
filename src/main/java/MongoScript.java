import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

import org.bson.Document;
import java.util.ArrayList;
import java.util.List;

public class MongoScript {

    public static void main(String[] args) {
        try (MongoClient mongoClient = MongoClients.create("mongodb+srv://apiuser:IggL9Oyj26ehmp83@production.n3ybxpg.mongodb.net")) {
            MongoDatabase database = mongoClient.getDatabase("hyp_backend_db");
            MongoCollection<Document> orderCollection = database.getCollection("orders");
            MongoCollection<Document> itemCollection = database.getCollection("items");
            MongoCollection<Document> attributeCollection = database.getCollection("attributes");

            // Step 1: Find orders with items missing item_attribute
            List<Document> ordersToUpdate = orderCollection.find(
                    Filters.and(
                            Filters.ne("items", new ArrayList<>()),
                            Filters.elemMatch("items", Filters.exists("item_attribute", false))
                    )
            ).limit(10).into(new ArrayList<>());

            
            System.out.println("Order to update "+ ordersToUpdate.size());

            

            for (Document order : ordersToUpdate) {
                

//                 Loop through each item in the order's items array
                List<Document> items = (List<Document>) order.get("items");
//                
                System.out.println("Order id "+ order.get("_id") + " item size "+items.size());
                for (Document item : items) {
//                	System.out.println("Order" + item);
           		 Document itemDoc = itemCollection.find(Filters.eq("_id", item.getString("item_id"))).first();
           		 if(itemDoc != null) {
           			 System.out.println("ItemDoc is not null " + itemDoc.get("item_attribute_id"));
           			item.put("item_attribute",itemDoc.get("item_attribute_id"));
           		 }else {
           			System.out.println("ItemDoc is null " +item.getString("item_id"));
           			item.put("item_attribute","1");
           		 }                
                }
                
                System.out.println("Items "+items);
                
           		 
                orderCollection.updateOne(
                        Filters.eq("_id", order.get("_id")),
                        Updates.set("items", items)
                );

            }

            System.out.println("Item attributes updated successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error during item attribute update: " + e.getMessage());
        }
    }
}
