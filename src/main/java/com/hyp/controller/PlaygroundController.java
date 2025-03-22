package com.hyp.controller;

import java.time.Duration;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hyp.enums.OrderStatusType;
import com.hyp.model.FacebookMessageResponse;
import com.hyp.request.FacebookMessageRequest;
import com.hyp.request.FileUploadRequest;
import com.hyp.request.MailNotificationRequest;
import com.hyp.request.OneSignalNotificationRequest;
import com.hyp.response.Response;
import com.hyp.service.BucketService;
import com.hyp.service.MailService;
import com.hyp.service.MetaService;
import com.hyp.service.NotificationService;

@RestController
@RequestMapping("/play-ground")
public class PlaygroundController {

	@Autowired
	MetaService metaService;
	
	@Autowired
	MailService mailService;
	
	@Autowired
	BucketService bucketService;
	
	@Autowired
    RedisTemplate<String, Object> redisTemplate;
	
	@Autowired
	NotificationService notificationService;

	
	
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
	
	@PostMapping("/send-mail")
	public ResponseEntity<Response> metaSendMessage(@RequestBody MailNotificationRequest mailNotificationRequest) {
		Response response;
		try {
			mailService.sendNotificationEmail(mailNotificationRequest);
			response = new Response(null, false, "Mail Sent !");
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			response = new Response(null, true, e.getMessage());
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}
	
	@PostMapping("/file-upload")
	public ResponseEntity<Response> fileUpload(@RequestBody FileUploadRequest fileUploadRequest) {
		Response response;
		try {
			bucketService.uploadFile(fileUploadRequest);
			response = new Response(null, false, "File Uploaded !");
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			response = new Response(null, true, e.getMessage());
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}
	
	@GetMapping("/redis-order/{orderId}")
	public ResponseEntity<Response> redisOrderTest(@PathVariable String orderId) {
		Response response;
		try {
			String redisKey = "order:" + orderId + ":state";
	        redisTemplate.opsForValue().set(redisKey, OrderStatusType.PAYMENT_PENDING.name(), Duration.ofMinutes(1));
			response = new Response(null, false, "Redis Key Set Successfully !");
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			response = new Response(null, true, e.getMessage());
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}
	
	@PostMapping("/one-signal/notification")
	public ResponseEntity<Response> sendNotification(@RequestBody OneSignalNotificationRequest oneSignalNotificationRequest) {
		Response response;
		try {
			notificationService.sendOneSignalNotification(oneSignalNotificationRequest);
			response = new Response(null, false, "Notification Sent !");
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			response = new Response(null, true, e.getMessage());
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}
}
