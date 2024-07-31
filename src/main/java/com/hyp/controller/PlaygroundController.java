package com.hyp.controller;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hyp.model.FacebookMessageResponse;
import com.hyp.request.FacebookMessageRequest;
import com.hyp.response.Response;
import com.hyp.service.MetaService;

@RestController
@RequestMapping("/play-ground")
public class PlaygroundController {

	@Autowired
	MetaService metaService;
	
	@PostMapping("/meta-message")
	public ResponseEntity<Response> metaSendMessage(@RequestBody FacebookMessageRequest facebookMessageRequest) {
		Response response;
		try {
			FacebookMessageResponse facebookMessageResponse = metaService.sendMessage(facebookMessageRequest);
			response = new Response(Collections.singletonList(facebookMessageResponse), false, "Meta Message Sent !");
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			response = new Response(null, true, e.getMessage());
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}
}
