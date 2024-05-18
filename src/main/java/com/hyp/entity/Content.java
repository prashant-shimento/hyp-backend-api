package com.hyp.entity;

import java.util.List;
import java.util.Map;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.hyp.enums.ContentType;

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
@Document(collection = "contents")
public class Content extends BaseEntity {

	private static final long serialVersionUID = 1L;

	private String title;
	@Field("image_url")
	private String imageUrl;
	private String description;
	private ContentType type;
	@Field("reference_ids")
	private Map<String, List<String>> referenceIds;

}
