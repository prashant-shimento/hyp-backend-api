package com.hyp.entity;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

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
@Document(collection = "categories")
public class Category extends BaseEntity {

	private static final long serialVersionUID = 1L;

    @Field("parent_category_id")
    private String parentCategoryId;

    @Field("category_image_url")
    private String categoryImageUrl;

    @Field("category_timings")
    private String categoryTimings;

    private String active;

    @Field("category_name")
    private String categoryName;

    @Field("category_rank")
    private String categoryRank;
}

