package com.hyp.entity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.hyp.annotation.GenerateId;
import com.hyp.enums.PartnerType;
import com.hyp.model.ApiConfig;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
@Document(collection = "partners")
public class Partner {

	@Id
	@Field("id")
	@GenerateId()
	private String id;

	private String name;
	private PartnerType type;
	private boolean isIntegrated;
	private Map<String, String> configs;

	@Field("api_config")
	private ApiConfig apiConfigs;

	@Field(name = "domain")
	private String domain;

	@Field("logo_url")
	private String logoUrl;

	@Field("web_url")
	private String webUrl;

	@Field("created_at")
	@CreatedDate
	private LocalDateTime createdAt = LocalDateTime.now();

	@Field("updated_at")
	@LastModifiedDate
	private LocalDateTime updatedAt;

	private List<String> restaurants;

	private transient List<Restaurant> restaurantDetails;

	@Field("header_image_urls")
	private String headerImageUrls;

	private String about;

	private String description;

	@Field("gallery_image_url")
	private List<String> galleryImageUrl;

	@Field("social_media_handles_link")
	private Map<String, String> socialMediaHandlesLink;

	@Field("office_address")
	private String officeAddress;

	private String contact;

	private String email;
}
