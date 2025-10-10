package com.hyp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class VerificationRequestDto {
    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid mobile number format")
    private String mobile;

    @NotNull(message = "OTP is required")
    private Integer otp;

    @NotBlank(message = "Restaurant ID is required")
    private String restaurantId;
}
