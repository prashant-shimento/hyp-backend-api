package com.hyp.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.hyp.annotation.GenerateId;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Document(collection = "permissions")
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(Include.NON_NULL)
@Builder
public class Permission {

    @Id
    @Field("id")
    @GenerateId()
    private String id;

    @Field("name")
    @Indexed(unique = true)
    private String name; // e.g., "menu:read", "order:create"

    @Field("display_name")
    private String displayName; // e.g., "Read Menu Items"

    @Field("description")
    private String description;

    @Field("category")
    private String category; // e.g., "MENU", "ORDER", "RESTAURANT"

    @Field("is_system")
    @Builder.Default
    private boolean isSystem = false; // Cannot be deleted

    @Field("active")
    @Builder.Default
    private boolean active = true;

    @Field("created_at")
    @CreatedDate
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Field("updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
