package com.hyp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import java.util.Set;
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

    private Set<String> restaurants;
}
