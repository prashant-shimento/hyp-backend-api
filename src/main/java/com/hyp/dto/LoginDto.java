package com.hyp.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginDto {
	@NotEmpty(message = "Name cannot be empty")
	private String name;
	@Pattern(regexp = "^\\d{10}$", message = "Invalid Mobile Number format. Please enter a 10-digit number.")
	private String mobile;
}
