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
@Document(collection = "parent_categories")
public class ParentCategory extends BaseEntity {

	private static final long serialVersionUID = 1L;

    private String name;
    
    private String rank;

    @Field("image_url")
    private String imageUrl;

    private String active;

    private String status;

}
