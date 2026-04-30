package com.hyp.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.hyp.response.PosResponse;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Hidden
@RestController
@RequestMapping("/api/v3/pos/urbanpiper")
public class UrbanPiperController {

    @PostMapping("/menu")
    public ResponseEntity<PosResponse> receiveMenu(@RequestBody JsonNode payload) {
        log.info("UrbanPiper menu payload received: {}", payload);
        return ResponseEntity.ok(PosResponse.builder()
                .success("1")
                .message("Menu received successfully")
                .build());
    }

    @PostMapping("/inventory")
    public ResponseEntity<PosResponse> receiveInventory(@RequestBody JsonNode payload) {
        log.info("UrbanPiper inventory payload received: {}", payload);
        return ResponseEntity.ok(PosResponse.builder()
                .success("1")
                .message("Inventory received successfully")
                .build());
    }

    @PostMapping("/order/status")
    public ResponseEntity<PosResponse> receiveOrderStatus(@RequestBody JsonNode payload) {
        log.info("UrbanPiper order status payload received: {}", payload);
        return ResponseEntity.ok(PosResponse.builder()
                .success("1")
                .message("Order status received successfully")
                .build());
    }

    @PostMapping("/store/status")
    public ResponseEntity<PosResponse> receiveStoreStatus(@RequestBody JsonNode payload) {
        log.info("UrbanPiper store status payload received: {}", payload);
        return ResponseEntity.ok(PosResponse.builder()
                .success("1")
                .message("Store status received successfully")
                .build());
    }
}
