package com.hyp.playground;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;

public class JsonModifier {
    public static void main(String[] args) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        
        // Read JSON from file or string
        File file = new File("src/main/resources/restaurant.json"); // Replace with your JSON file
        JsonNode rootNode = objectMapper.readTree(file);

        
        if (rootNode.has("items") && rootNode.get("items").isArray()) {
            for (JsonNode item : rootNode.get("items")) {
                // Check if "variation" exists and is an array
                if (item.has("variation") && item.get("variation").isArray()) {
                    for (JsonNode variation : item.get("variation")) {
                        // Ensure we modify an ObjectNode
                        if (variation instanceof ObjectNode) {
                            ObjectNode variationObject = (ObjectNode) variation;

                            if (!variationObject.has("variationid") && variationObject.has("id")) {
                                variationObject.set("variationid", variationObject.get("id"));
                            }
                            // Add "addon": [] if it doesn't exist
                            if (!variationObject.has("addon")) {
                                variationObject.set("addon", objectMapper.createArrayNode());
                            }
                        }
                    }
                }
            }
        }

        // Write back to file or print
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, rootNode);

        System.out.println("JSON modified successfully!");  
    }

}