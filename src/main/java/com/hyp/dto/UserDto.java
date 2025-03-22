package com.hyp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(Include.NON_NULL)
public class UserDto extends BaseDto {
	
	@NotEmpty(message = "Name cannot be empty")
    private String name;

    @Pattern(regexp = "^\\d{10}$", message = "Mobile number must be exactly 10 digits")
    private String mobile;

    @Email(message = "Invalid Email Address format. Please enter a valid email address.")
    @NotEmpty(message = "Email cannot be empty")
    private String email;
    
    @NotEmpty(message = "Password cannot be empty")
    private String password;
    
    @NotEmpty(message = "RestaurantId cannot be empty")
    private String restaurantId;
    
    @NotEmpty(message = "PartnerIds cannot be empty")
    private String partnerId;
    
    private Boolean active;
	
}