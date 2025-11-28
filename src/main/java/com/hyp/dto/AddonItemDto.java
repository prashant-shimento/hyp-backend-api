package com.hyp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@ToString
@NoArgsConstructor
@JsonInclude(Include.NON_NULL)
public class AddonItemDto extends BaseDto {

    @NotBlank(message = "Addon Item Name cannot be empty")
    private String addonItemName;

    @NotBlank(message = "Addon Item price cannot be empty")
    private String addonItemPrice;

    private String active = "1";

    @NotBlank(message = "Attribute ID cannot be empty")
    @Pattern(regexp = "[123]", message = "Attribute must be one of the following values: 1, 2, 3")
    private String attributes;

    private String addonItemRank;

    @NotBlank(message = "Selection Max cannot be null")
    @Pattern(regexp = "\\d+", message = "Selection Max must be a valid integer")
    private String addonItemSelectionMin;

    @NotBlank(message = "Selection Max cannot be null")
    @Pattern(regexp = "\\d+", message = "Selection Min must be a valid integer")
    private String addonItemSelectionMax;

    private LocalDateTime autoTurnOnTime;
}
