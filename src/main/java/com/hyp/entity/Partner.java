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
import org.springframework.data.mongodb.core.mapping.DBRef;
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

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "partners")
public class Partner {

	@Id
	@Field("id")
	@GenerateId()
	private String id;

	private String name;
	private PartnerType type;
	private boolean isIntegrated;
	private Map<String, List<String>> configs;
	@Field("api_config")
	private ApiConfig apiConfigs;

	@Field("created_at")
	@CreatedDate
	private LocalDateTime createdAt;

	@Field("updated_at")
	@LastModifiedDate
	private LocalDateTime updatedAt;
	
	@DBRef(db = "restaurants")
	private List<Restaurant> restaurants;
	
	@PrePersist
	@PreUpdate
	private void encryptApiConfigData() {
		try {
			if (this.apiConfigs != null) {
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
