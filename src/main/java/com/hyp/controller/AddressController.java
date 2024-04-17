package com.hyp.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hyp.dto.AddressDto;
import com.hyp.entity.Address;
import com.hyp.entity.Customer;
import com.hyp.entity.Address.Location;
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
	public ResponseEntity<Response> addAddress(@RequestBody List<AddressDto> addressDtoList,
			@PathVariable String customerId) {
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

			List<Address> addresses = createAddresses(addressDtoList);

			customer.setVerified(true);
			customer.setAddresses(addresses);
			customerService.save(customer);

			response = new Response(Collections.singletonList(customer), false, "Address Added Successfully");
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			response = new Response(null, true, e.getMessage());
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}

	private List<Address> createAddresses(List<AddressDto> addressDtoList) {
		List<Address> addresses = new ArrayList<>();
		for (AddressDto addressDto : addressDtoList) {
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
			address = addressService.save(address);
			addresses.add(address);
		}
		return addresses;
	}
}
