package com.hyp.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "parent_categories")
public class ParentCategory extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Field("parent_category_id")
    private String parentCategoryId;

    private String name;

    private String rank;

    @Field("image_url")
    private String imageUrl;

    private String active;

    private String status;
}
