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

import com.hyp.dto.AddressDto;
import com.hyp.dto.CustomerDto;
import com.hyp.entity.Address;
import com.hyp.entity.Customer;
import com.hyp.response.Response;
import com.hyp.service.AddressService;
import com.hyp.service.CustomerService;
import com.hyp.translation.AddressTranslation;
import com.hyp.translation.CustomerTranslation;

@RestController
@RequestMapping("/customer")
public class CustomerController extends BaseController<CustomerDto, Customer, String> {
	
	@Autowired
	public CustomerTranslation customerTranslation;
	
	@Autowired
	public AddressTranslation addressTranslation;

	@Autowired
	public AddressService addressService;

	@Autowired
	public CustomerService customerService;

	@PostMapping("/{customerId}/address")
	public ResponseEntity<Response> addAddress(@RequestBody AddressDto addressDto, @PathVariable String customerId) {
		Response response;
		try {
			Customer customer = customerService.findById(customerId);
			if (customer == null) {
				response = new Response(null, true, "Customer not found");
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
			}

			if (!customer.isVerified()) {
				response = new Response(null, true, "Verification Required");
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
			}

			Address address = addressTranslation.getEntity(addressDto);
			addressService.save(address);
			customer.getAddresses().add(address);
			customerService.save(customer);

			response = new Response(Collections.singletonList(customerTranslation.getDto(customer)), false,
					"Address Added Successfully");
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			response = new Response(null, true, e.getMessage());
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}
}
