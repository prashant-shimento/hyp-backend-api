import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
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
            ).into(new ArrayList<>());

            
            
            

            for (Document order : ordersToUpdate) {
                List<Document> updatedItems = new ArrayList<>();

                // Loop through each item in the order's items array
                List<Document> items = (List<Document>) order.get("items");
                
                for (Document item : items) {
                	if(!item.containsKey("item_attribute")) {
                		System.out.println(item.get("item_id") + " doesn't contain attributes");
                		 Document itemDoc = itemCollection.find(Filters.eq("_id", item.getString("item_id"))).first();
                  		System.out.println(item.toJson() + " before adding attributes ");

                         if (itemDoc != null && itemDoc.containsKey("item_attribute_id")) {
                         	item.put("item_attribute",itemDoc.get("item_attribute_id"));
                     		System.out.println(item.toJson() + " after adding attributes ");

                         }

                         updatedItems.add(item);
                	}                        
                }
                System.out.println(updatedItems.size() + "to be updated ");

//                orderCollection.updateOne(
//                        Filters.eq("_id", order.get("_id")),
//                        Updates.set("items", updatedItems)
//                );

            }

            System.out.println("Item attributes updated successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error during item attribute update: " + e.getMessage());
        }
    }
}
