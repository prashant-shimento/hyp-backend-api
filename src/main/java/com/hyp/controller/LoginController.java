package com.hyp.controller;

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

import com.hyp.constants.Constants;
import com.hyp.dto.LoginDto;
import com.hyp.dto.VerificationRequestDto;
import com.hyp.entity.Customer;
import com.hyp.response.Response;
import com.hyp.service.AddressService;
import com.hyp.service.CustomerService;
import com.hyp.service.OtpService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/login")
public class LoginController {

	@Value("${INTERNAL_USER_NUMBERS}")
	private String internalUserNumbers;

	@Autowired
	public CustomerService customerService;

	@Autowired
	public AddressService addressService;

	@Autowired
	OtpService otpService;

	@Autowired
	private Environment env;

	@Autowired
	HttpServletRequest httpRequest;

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

			// here we will Check if the customer is an internal user
			boolean isInternalUser = checkIfInternalUser(customer.getMobile());

			if (isInternalUser) {
				// Log message for internal users and skip actual OTP sending
				System.out.println("Default OTP for internal user: OTP sent successfully.");
				response = new Response(null, false, "OTP Sent Successfully");
			} else {
				// Generate and send real OTP for external users
				otpService.sendOtp(customer.getMobile());
				response = new Response(null, false, "OTP Sent Successfully");
			}

			return ResponseEntity.ok(response);
		} catch (Exception e) {
			response = new Response(null, true, e.getMessage());
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}

	@PostMapping("/verify-otp")
	public ResponseEntity<Response> otpVerify(@RequestBody VerificationRequestDto verificationRequest) {
		Response response;
		try {

			if (checkIfInternalUser(verificationRequest.getMobile())) {
				// For internal users
				Customer customer = customerService.findByMobile(verificationRequest.getMobile());
				if (customer != null) {
					customer.setVerified(true);
					customerService.save(customer);
					response = new Response(Collections.singletonList(customer), false, "OTP Verified Successfully");
					return ResponseEntity.ok(response);
				} else {
					response = new Response(null, true, "Customer not found");
					return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
				}
			} else {
				// For external users, verify against the real OTP
				int storedOtp = otpService.getOtp(verificationRequest.getMobile());
				if (verificationRequest.getOtp() == storedOtp) {
					Customer customer = customerService.findByMobile(verificationRequest.getMobile());
					if (customer != null) {
						customer.setVerified(true);
						customerService.save(customer);
						response = new Response(Collections.singletonList(customer), false,
								"OTP Verified Successfully");
						return ResponseEntity.ok(response);
					} else {
						response = new Response(null, true, "Customer not found");
						return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
					}
				} else {
					response = new Response(null, true, "OTP Verification Failed");
					return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
				}
			}
		} catch (Exception e) {
			response = new Response(null, true, e.getMessage());
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}

	private boolean checkIfInternalUser(String mobile) {
		return internalUserNumbers.contains(mobile);
	}

	@PostMapping("/resend-otp/{mobile}")
	public ResponseEntity<Response> otpResend(@PathVariable String mobile) {
		Response response;
		try {
			otpService.clearOTP(mobile);
			otpService.sendOtp(mobile);
			response = new Response(null, false, "OTP Sent Successfully");
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			response = new Response(null, true, e.getMessage());
			e.printStackTrace();
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

		return configMap;
	}

}
