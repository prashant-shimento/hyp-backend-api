package com.hyp.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.hyp.constants.ErrorConstants;
import com.hyp.dto.LoginDto;
import com.hyp.dto.VerificationRequestDto;
import com.hyp.entity.Customer;
import com.hyp.response.Response;
import com.hyp.service.AddressService;
import com.hyp.service.CustomerService;
import com.hyp.service.OtpService;
import com.hyp.service.RedisService;
import com.hyp.service.RestaurantService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/login")
public class LoginController {

	@Autowired
	public CustomerService customerService;

	@Autowired
	public RestaurantService restaurantService;

	@Autowired
	public AddressService addressService;

	@Autowired
	OtpService otpService;

	@Autowired
	private Environment env;

	@Autowired
	HttpServletRequest httpRequest;
	
	@Autowired
	RedisService redisService;

	@PostMapping("/otp")
	public ResponseEntity<Response> userLogin(@RequestBody LoginDto loginDto) {
		Response response;
		try {
			Customer customer = customerService.findByMobile(loginDto.getMobile());
			if (customer == null) {
				customer = new Customer();
				customer.setName(loginDto.getName());
				customer.setMobile(loginDto.getMobile());
				customer = customerService.save(customer);
			}
			if (!checkIfInternalUser(customer.getMobile())) {
				otpService.sendOtp(customer.getMobile());
			}
			response = new Response(null, false, "OTP Sent Successfully");
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			log.error("Exception occurred in userLogin " + e.getMessage());
			response = new Response(null, true, ErrorConstants.INTERNAL_SERVER_ERROR);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}

	@PostMapping("/verify-otp")
	public ResponseEntity<Response> otpVerify(@RequestBody VerificationRequestDto verificationRequest) {
		Response response;
		try {
			Customer customer = customerService.findByMobile(verificationRequest.getMobile());
			if (customer == null) {
				otpService.clearOTP(verificationRequest.getMobile());
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Response(null, true, "Customer not found"));
			}
			if (!restaurantService.isExistsById(verificationRequest.getRestaurantId())) {
				otpService.clearOTP(verificationRequest.getMobile());
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(new Response(null, true, "Restaurant not found"));
			}

			if (!checkIfInternalUser(verificationRequest.getMobile())) {
				int storedOtp = otpService.getOtp(verificationRequest.getMobile());
				log.info("storedOTP {} requestedOTP {}", storedOtp, verificationRequest.getOtp());
				if (verificationRequest.getOtp() != storedOtp) {
					return ResponseEntity.status(HttpStatus.BAD_REQUEST)
							.body(new Response(null, true, "OTP Verification Failed"));
				}
				otpService.clearOTP(verificationRequest.getMobile());

			}
			if (customer.getRestaurants() == null) {
				customer.setRestaurants(new ArrayList<>());
			}
			if (!customer.getRestaurants().contains(verificationRequest.getRestaurantId())) {
				customer.getRestaurants().add(verificationRequest.getRestaurantId());
			}
			customer.setVerified(true);
			customerService.save(customer);

			return ResponseEntity
					.ok(new Response(Collections.singletonList(customer), false, "OTP Verified Successfully"));
		} catch (Exception e) {
			log.error("Exception occurred in otpVerify " + e.getMessage());
			response = new Response(null, true, ErrorConstants.INTERNAL_SERVER_ERROR);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}

	private boolean checkIfInternalUser(String mobile) {
		return redisService.getInternalUsers().contains(mobile);
	}

	@PostMapping("/resend-otp/{mobile}")
	public ResponseEntity<Response> otpResend(@PathVariable String mobile) {
		Response response;
		try {
			if (!checkIfInternalUser(mobile)) {
				otpService.clearOTP(mobile);
				otpService.sendOtp(mobile);
			}
			response = new Response(null, false, "OTP Sent Successfully");
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			log.error("Exception occurred in otpResend " + e.getMessage());
			response = new Response(null, true, ErrorConstants.INTERNAL_SERVER_ERROR);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}

	@GetMapping("/config")
	public Map<String, Object> getConfig() {
		Map<String, Object> configMap = new HashMap<String, Object>();
		configMap.put("sms.url", env.getProperty("sms.url"));
		configMap.put("sms.key", env.getProperty("sms.key"));
		configMap.put("delivery.pidge.url", env.getProperty("delivery.pidge.url"));
		configMap.put("delivery.pidge.username", env.getProperty("delivery.pidge.username"));
		configMap.put("delivery.pidge.password", env.getProperty("delivery.pidge.password"));
		configMap.put("delivery.pidge.token", env.getProperty("delivery.pidge.token"));
		configMap.put("google.api.key", env.getProperty("google.api.key"));
		configMap.put("pos.petpooja.url", env.getProperty("pos.petpooja.url"));
		configMap.put("pos.petpooja.token", env.getProperty("pos.petpooja.token"));
		configMap.put("pos.petpooja.secret", env.getProperty("pos.petpooja.secret"));
		configMap.put("pos.petpooja.key", env.getProperty("pos.petpooja.key"));
		configMap.put("razorpay.key", env.getProperty("razorpay.key"));
		configMap.put("razorpay.secret", env.getProperty("razorpay.secret"));
		configMap.put("app.domain", env.getProperty("app.domain"));
		configMap.put("spring.data.mongodb.uri", env.getProperty("spring.data.mongodb.uri"));
		String scheme = httpRequest.getScheme();
		String baseUrl = ServletUriComponentsBuilder.fromRequestUri(httpRequest).replacePath(null).build()
				.toUriString();
		configMap.put("domain", httpRequest.getRequestURL().toString() + "======" + scheme + "======" + baseUrl);
		configMap.put("internal.users.numbers", env.getProperty("internal.users.numbers"));

		return configMap;
	}

}
