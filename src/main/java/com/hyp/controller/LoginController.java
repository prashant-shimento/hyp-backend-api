package com.hyp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hyp.dto.CustomerDto;
import com.hyp.dto.VerificationRequestDto;
import com.hyp.entity.Customer;
import com.hyp.response.ResponseTemplate;
import com.hyp.service.CustomerService;
import com.hyp.service.OtpService;

@RestController
@RequestMapping("/login")
public class LoginController {

	@Autowired
	CustomerService customerService;

	@Autowired
	OtpService otpService;

	@PostMapping("/otp")
	public ResponseEntity<ResponseTemplate> userLogin(@RequestBody CustomerDto customerDto) {
		ResponseTemplate response;
		try {
			Customer customer = customerService.findByMobile(customerDto.getMobile());
			if (customer == null) {
				customer = new Customer();
				customer.setMobile(customerDto.getMobile());
				customer.setName(customerDto.getName());
				customer = customerService.save(customer);
			}
			otpService.sendOtp(customer.getMobile());

			response = new ResponseTemplate(null, false, "OTP Sent Successfully");
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			response = new ResponseTemplate(null, true, e.getMessage());
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}

	@PostMapping("/verify-otp")
	public ResponseEntity<ResponseTemplate> otpVerify(@RequestBody VerificationRequestDto verificationRequest) {
		ResponseTemplate response;
		try {
			int storedOtp = otpService.getOtp(verificationRequest.getMobile());
			if (verificationRequest.getOtp() == storedOtp) {
				response = new ResponseTemplate(null, false, "OTP Sent Successfully");
				return ResponseEntity.ok(response);
			} else {
				response = new ResponseTemplate(null, true, "OTP Verification Failed");
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
			}
		} catch (Exception e) {
			response = new ResponseTemplate(null, true, e.getMessage());
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}
	
	@PostMapping("/resend-otp")
	public ResponseEntity<ResponseTemplate> otpResend(@RequestBody CustomerDto customerDto) {
		ResponseTemplate response;
		try {
			otpService.clearOTP(customerDto.getMobile());
			otpService.sendOtp(customerDto.getMobile());
			response = new ResponseTemplate(null, false, "OTP Sent Successfully");
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			response = new ResponseTemplate(null, true, e.getMessage());
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}
}
