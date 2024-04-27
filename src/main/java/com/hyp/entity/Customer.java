package com.hyp.entity;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Document(collection = "customers")
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(Include.NON_NULL)
public class Customer extends BaseEntity {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@Field("name")
	private String name;
	@Field("mobile")
	private String mobile;
	@Field("email")
	private String email;
	@Field("is_verified")
	private boolean isVerified;
	@DBRef
	private List<Address> addresses = new ArrayList<>();

}
