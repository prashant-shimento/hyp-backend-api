package com.hyp.entity;

import java.util.List;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Document(collection = "customers")
@AllArgsConstructor
@NoArgsConstructor
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
	@Field("address_id") 
	private List<String> address_id;
	
	private transient List<Address> addresses;
	
	private transient Address address;

	
	
}
