package com.hyp.controller;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hyp.dto.LoginDto;
import com.hyp.dto.VerificationRequestDto;
import com.hyp.entity.Customer;
import com.hyp.mapper.DataMapper;
import com.hyp.response.Response;
import com.hyp.service.AddressService;
import com.hyp.service.CustomerService;
import com.hyp.service.OtpService;
import com.hyp.util.CommonUtils;

@RestController
@RequestMapping("/login")
public class LoginController {

	@Autowired
	public CustomerService customerService;

	@Autowired
	public AddressService addressService;

	@Autowired
	OtpService otpService;

	@Autowired
	DataMapper dataMapper;

	@PostMapping("/otp")
	public ResponseEntity<Response> userLogin(@RequestBody LoginDto loginDto) {
		Response response;
		try {
			Customer customer = customerService.findByMobile(loginDto.getMobile());
			if (customer == null) {
				customer = new Customer();
				customer.setId(CommonUtils.genId());
				customer.setName(loginDto.getName());
				customer.setMobile(loginDto.getMobile());
				customer = customerService.save(customer);
			}

			otpService.sendOtp(customer.getMobile());

			response = new Response(null, false, "OTP Sent Successfully");
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
			int storedOtp = otpService.getOtp(verificationRequest.getMobile());
			if (verificationRequest.getOtp() == storedOtp) {
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
				response = new Response(null, true, "OTP Verification Failed");
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
			}
		} catch (Exception e) {
			response = new Response(null, true, e.getMessage());
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
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

}
