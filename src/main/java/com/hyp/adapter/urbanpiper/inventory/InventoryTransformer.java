package com.hyp.adapter.urbanpiper.inventory;

import com.hyp.request.PosStockRequest;
import com.hyp.request.urbanpiper.InventoryRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class InventoryTransformer {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public PosStockRequest transform(InventoryRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("InventoryRequest cannot be null");
        }

        try {
            PosStockRequest stockRequest = new PosStockRequest();

            stockRequest.setRestaurantId(request.getLocationRefId());
            stockRequest.setInStock(request.getInStock());

            stockRequest.setItemId(collectIds(request));

            stockRequest.setAutoTurnOnTime(formatTime(request.getNextAvailableAt()));

            stockRequest.setType("item");

            return stockRequest;

        } catch (Exception e) {
            log.error("Error transforming inventory payload: {}", request, e);
            throw new RuntimeException("Failed to transform inventory payload", e);
        }
    }

    private List<String> collectIds(InventoryRequest request) {
        List<String> ids = new ArrayList<>();

        if (request.getItems() != null) {
            ids.addAll(request.getItems());
        }

        if (request.getVariants() != null) {
            ids.addAll(request.getVariants());
        }

        if (request.getAddOns() != null) {
            ids.addAll(request.getAddOns());
        }

        return ids;
    }

    private String formatTime(Long epochSeconds) {
        if (epochSeconds == null) {
            return null;
        }

        return Instant.ofEpochSecond(epochSeconds)
                .atZone(ZoneId.systemDefault())
                .format(FORMATTER);
    }
}