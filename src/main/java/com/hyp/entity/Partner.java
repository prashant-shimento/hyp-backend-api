package com.hyp.entity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import javax.persistence.PostLoad;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.hyp.annotation.GenerateId;
import com.hyp.enums.PartnerType;
import com.hyp.model.ApiConfig;
import com.hyp.util.EncryptionUtils;

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
	private LocalDateTime createdAt = LocalDateTime.now();;

	@Field("updated_at")
	@LastModifiedDate
	private LocalDateTime updatedAt;
	
	private List<String> restaurants;
	
	private transient List<Restaurant> restaurantDetails;
	
	@PrePersist
	@PreUpdate
	private void encryptApiConfigData() {
		try {
            log.info("Encrypting ApiConfig data: {}", this.configs);
            System.out.println(this.configs.get("AccessToken"));
            System.out.println(this.configs.get("faceBookBusinessId"));

			if (this.apiConfigs != null) {
	            log.info("Encrypting ApiConfig data: {}", this.apiConfigs);
				this.apiConfigs.setSecret(EncryptionUtils.encrypt(this.apiConfigs.getSecret()));
				this.apiConfigs.setToken(EncryptionUtils.encrypt(this.apiConfigs.getToken()));
				this.apiConfigs.setKey(EncryptionUtils.encrypt(this.apiConfigs.getKey()));
				this.apiConfigs.setPassword(EncryptionUtils.encrypt(this.apiConfigs.getPassword()));
			}
		} catch (Exception e) {
			throw new RuntimeException("Error encrypting ApiConfig data", e);
		}
	}

	@PostLoad
	private void decryptApiConfigData() {
		try {
			  System.out.println(this.configs.get("AccessToken"));
	            System.out.println(this.configs.get("faceBookBusinessId"));

			if (this.apiConfigs != null) {
				this.apiConfigs.setSecret(EncryptionUtils.decrypt(this.apiConfigs.getSecret()));
				this.apiConfigs.setToken(EncryptionUtils.decrypt(this.apiConfigs.getToken()));
				this.apiConfigs.setKey(EncryptionUtils.decrypt(this.apiConfigs.getKey()));
				this.apiConfigs.setPassword(EncryptionUtils.decrypt(this.apiConfigs.getPassword()));
			}
		} catch (Exception e) {
			throw new RuntimeException("Error decrypting ApiConfig data", e);
		}
	}
}