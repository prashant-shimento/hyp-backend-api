package com.hyp.dto;

import java.util.List;
import java.util.Map;

import com.hyp.enums.ContentType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContentDto extends BaseDto {

	@NotBlank(message = "Title is required")
	private String title;

	@NotBlank(message = "Image URL is required")
	private String imageUrl;

	@NotBlank(message = "Description is required")
	private String description;

	@NotNull(message = "Type is required")
	private ContentType type;

	private Map<String, List<String>> referenceIds;
}
