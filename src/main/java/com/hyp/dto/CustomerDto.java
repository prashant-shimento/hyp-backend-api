package com.hyp.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.hyp.entity.Address;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(Include.NON_NULL)
public class CustomerDto extends BaseDto {
	@NotEmpty(message = "Name cannot be empty")
	private String name;
	@Pattern(regexp = "^\\d{10}$", message = "Invalid Mobile Number format. Please enter a 10-digit number.")
	private String mobile;
	@Email(message = "Invalid Email Address format. Please enter a valid email address.")
	private String email;
	@NotNull(message = "Address list cant be empty")
	private List<Address> addresses;
	@NotNull(message = "Address cant be empty")
	private Address address;

}