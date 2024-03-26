package com.hyp.dto;

import java.util.List;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerDto extends BaseDto{
	@NotEmpty(message = "Name cannot be empty")
	private String name;
    @Pattern(regexp = "^\\d{10}$", message = "Invalid Mobile Number format. Please enter a 10-digit number.")
	private String mobile;
    @Email(message = "Invalid Email Address format. Please enter a valid email address.")
	private String email;
    @NotEmpty(message = "Address ID list cannot be empty")
	private List<String> address_id;
}