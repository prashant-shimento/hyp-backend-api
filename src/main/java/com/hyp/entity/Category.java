package com.hyp.entity;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "categories")
@CompoundIndex(name = "idx_restaurant_deleted", def = "{'restaurant_id': 1, 'is_deleted': 1}")
public class Category extends BaseEntity {

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

    private transient List<Item> items;

    @Field("source_id")
    private String sourceId;
}
