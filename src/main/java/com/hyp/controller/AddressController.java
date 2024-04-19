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
import com.hyp.entity.Address.Location;
import com.hyp.entity.Customer;
import com.hyp.mapper.DataMapper;
import com.hyp.response.Response;
import com.hyp.service.AddressService;
import com.hyp.service.CustomerService;
import com.hyp.service.OtpService;
import com.hyp.translation.AddressTranslation;
import com.hyp.util.CommonUtils;

@RestController
@RequestMapping("/address")
public class AddressController extends BaseController<AddressDto, Address, String> {

	@Autowired
	public AddressTranslation addressTranslation;

	@Autowired
	public AddressService addressService;

	@Autowired
	public CustomerService customerService;

	@Autowired
	public OtpService otpService;

	@PostMapping("/{customerId}")
	public ResponseEntity<Response> addAddress(@RequestBody AddressDto addressDto, @PathVariable String customerId) {
		Response response;
		try {
			Customer customer = customerService.findById(customerId);
			if (customer == null) {
				response = new Response(null, true, "Customer not found");
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
			}

			int storedOtp = otpService.getOtp(customer.getMobile());
			if (storedOtp == -1) {
				response = new Response(null, true, "OTP Verification Required");
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
			}

			Address address = createAddress(addressDto);

			customer.getAddresses().add(address);
			customerService.save(customer);

			CustomerDto customerResponseDto = new CustomerDto();
			customerResponseDto.setId(customer.getId());
			customerResponseDto.setName(customer.getName());
			customerResponseDto.setMobile(customer.getMobile());
			customerResponseDto.setAddresses(Collections.singletonList(address));

			response = new Response(Collections.singletonList(customerResponseDto), false,
					"Address Added Successfully");
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			response = new Response(null, true, e.getMessage());
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}

	private Address createAddress(AddressDto addressDto) {
		Address address = new Address();
		address.setId(CommonUtils.genId());
		address.setAddressOne(addressDto.getAddressOne());
		address.setAddressTwo(addressDto.getAddressTwo());
		address.setLandmark(addressDto.getLandmark());
		address.setCity(addressDto.getCity());
		address.setState(addressDto.getState());
		address.setCountry(addressDto.getCountry());
		address.setPincode(addressDto.getPincode());
		address.setLocation(new Location(addressDto.getLatitude(), addressDto.getLongitude()));
		return addressService.save(address);
	}

}
