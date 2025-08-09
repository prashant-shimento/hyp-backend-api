package com.hyp.dto;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hyp.entity.Restaurant;
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
@JsonInclude(Include.NON_NULL)
public class PartnerDto {

	private String id;
	@NotBlank(message = "Name is required")
	private String name;

	@NotNull(message = "Type is required")
	private PartnerType type;

	private boolean isIntegrated;

	@NotNull(message = "Domain is required")
	private String domain;

	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	private Map<String, String> configs;

	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	private ApiConfig apiConfigs;

	private List<String> restaurants;

	private String createdAt;
	private String updatedAt;
	private List<Restaurant> restaurantDetails;

	@NotNull(message = "LogoUrl is required")
	private String logoUrl;

	@NotNull(message = "WebUrl is required")
	private String webUrl;

	private String headerImageUrls;

	private String about;

	private String description;

	private List<String> galleryImageUrl;

	private Map<String, String> socialMediaHandlesLink;

	private String officeAddress;

	private String contact;

	private String email;
}
