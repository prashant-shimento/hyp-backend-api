package com.hyp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class CategoryDto extends BaseDto {
    private String active = "1";

    private String categoryRank;

    private String parentCategoryId;

    @NotBlank(message = "Category name cannot be empty")
    @Size(max = 255, message = "Category name must be at most 255 characters")
    private String categoryName;

    private String categoryTimings;

    private String categoryImageUrl;
}
