package com.hyp.playground;

import com.mongodb.client.*;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.UnwindOptions;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.bson.Document;

public class PaymentUpdate {
    public static void main(String[] args) {
        try (MongoClient mongoClient =
                MongoClients.create("mongodb+srv://apiuser:IggL9Oyj26ehmp83@production.n3ybxpg.mongodb.net")) {
            MongoDatabase database = mongoClient.getDatabase("hyp_backend_db");
            MongoCollection<Document> orderCollection = database.getCollection("orders");

            // Aggregation pipeline equivalent to the provided query
            List<Document> results = orderCollection
                    .aggregate(Arrays.asList(
                            // Step 1: Match orders with status "PAYMENT_PENDING"
                            Aggregates.match(Filters.eq("status", "PAYMENT_PENDING")),

                            // Step 2: Lookup payments collection for payment info
                            Aggregates.lookup("payments", "_id", "order_id", "payment_info"),

                            // Step 3: Unwind payment_info array
                            Aggregates.unwind("$payment_info", new UnwindOptions().preserveNullAndEmptyArrays(true)),
                            Aggregates.sort(Sorts.ascending("created_at")),
                            // Step 4: Project specific fields
                            Aggregates.project(Projections.fields(
                                    Projections.include("order_id", "payment_info.payment_order_id")))))
                    .into(new ArrayList<>());

            // Print the results
            System.out.println("Aggregation Results:" + results.size());
            List<String> paymentOrderIds = results.stream()
                    .map(result -> {
                        Document paymentDocument = (Document) result.get("payment_info");
                        return paymentDocument != null ? paymentDocument.getString("payment_order_id") : null;
                    })
                    .filter(paymentOrderId -> paymentOrderId != null) // Filter out null values
                    .collect(Collectors.toList());
            System.out.println("Payment Order IDs: " + paymentOrderIds);
            Map<String, String> orderIdStatusMap = paymentOrderIds.stream()
                    .collect(Collectors.toMap(orderId -> orderId, orderId -> {
                        try {
                            RazorpayClient razorpayClient =
                                    new RazorpayClient("rzp_live_oUdd0v4WZIGGRP", "vX3Ov1gnhzox3LeHEHANxPO8");
                            Order order = razorpayClient.orders.fetch(orderId);
                            System.out.println("orderId :" + orderId + "- : " + order.get("status"));
                            return (String) order.get("status"); // Fetch status
                        } catch (Exception e) {
                            e.printStackTrace();
                            return "Error fetching status"; // Handle errors gracefully
                        }
                    }));

            // Print the map
            System.out.println("Order ID and Status Map Size: " + orderIdStatusMap.size());

            System.out.println("Order ID and Status Map: " + orderIdStatusMap);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error during item price reversion: " + e.getMessage());
        }
    }
}
