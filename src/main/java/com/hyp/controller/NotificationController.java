package com.hyp.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hyp.exception.EntityNotFoundException;
import com.hyp.exception.OneSignalException;
import com.hyp.response.Response;
import com.hyp.service.UserService;

@RestController
@RequestMapping("/notification")
public class NotificationController {

	@Autowired
	UserService userService;

	@PostMapping("/one-signal/register/{internalId}")
	public ResponseEntity<Response> login(@PathVariable String internalId, @RequestParam String externalId)
			throws EntityNotFoundException, OneSignalException {
		Optional.ofNullable(userService.findById(internalId))
				.orElseThrow(() -> new EntityNotFoundException("User", internalId));

		if (externalId == null || externalId.isEmpty()) {
			return ResponseEntity.badRequest().body(new Response(null, true, "playerId is required"));
		}
		userService.registerOneSignalUser(internalId, externalId);
		return ResponseEntity.ok(new Response(null, false, "Registration successful"));
	}

	@DeleteMapping("/one-signal/register/{internalId}")
	public ResponseEntity<Response> deleteRegistration(@PathVariable String internalId, @RequestParam String externalId)
			throws EntityNotFoundException, OneSignalException {

		Optional.ofNullable(userService.findById(internalId))
				.orElseThrow(() -> new EntityNotFoundException("User", internalId));

		if (externalId == null || externalId.isEmpty()) {
			return ResponseEntity.badRequest().body(new Response(null, true, "playerId is required"));
		}

		userService.unregisterOneSignalUser(internalId, externalId);
		return ResponseEntity.ok(new Response(null, false, "Registration Deleted successful"));
	}

}
