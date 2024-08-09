package com.hyp.dto;

import java.util.List;
import java.util.Map;

import com.hyp.enums.ContentType;
import com.hyp.enums.PartnerType;
import com.hyp.model.ApiConfig;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PartnerDto extends BaseDto {

	@NotBlank(message = "Name is required")
	private String name;

	@NotBlank(message = "Partner Id is required")
	private String partnerId;

	@NotNull(message = "Type is required")
	private PartnerType type;

	private boolean isIntegrated;

	private Map<String, String> configs;

	private ApiConfig apiConfigs;
}
