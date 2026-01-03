package com.hyp.controller;

import com.hyp.service.CategoryService;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/ondc")
@RequiredArgsConstructor
public class OndcController {

    private final CategoryService categoryService;

    @PostMapping("/search")
    public ResponseEntity<Map<String, Object>> onSearch(@RequestBody Map<String, Object> requestBody) {
        log.info("ONDC search callback received");

        Map<String, Object> ack = new HashMap<>();
        ack.put("status", "ACK");

        Map<String, Object> message = new HashMap<>();
        message.put("ack", ack);

        Map<String, Object> response = new HashMap<>();
        response.put("message", message);
        return ResponseEntity.ok(response);
    }
}
