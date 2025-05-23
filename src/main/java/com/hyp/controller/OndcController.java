package com.hyp.controller;

import com.hyp.service.CategoryService;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Data
@RestController
@RequestMapping("/ondc")
public class OndcController {

	private final CategoryService categoryService;

	@PostMapping("/search")
	public ResponseEntity<Map<String, Object>> onSearch(@RequestBody Map<String, Object> requestBody) {
		System.out.println("Received /search callback:");
		System.out.println(requestBody);

		Map<String, Object> ack = new HashMap<>();
		ack.put("status", "ACK");

		Map<String, Object> message = new HashMap<>();
		message.put("ack", ack);

		Map<String, Object> response = new HashMap<>();
		response.put("message", message);
		return ResponseEntity.ok(response);
	}
}