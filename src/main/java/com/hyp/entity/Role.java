package com.hyp.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.hyp.annotation.GenerateId;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
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
@Document(collection = "roles")
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(Include.NON_NULL)
@Builder
public class Role {

    @Id
    @Field("id")
    @GenerateId()
    private String id;

    @Field("name")
    @Indexed(unique = true)
    private String name;

    @Field("display_name")
    private String displayName;

    @Field("description")
    private String description;

    @Field("priority")
    private int priority;

    @Field("inherits_from")
    private Set<String> inheritsFrom;

    @Field("permissions")
    @Builder.Default
    private Set<String> permissions = new HashSet<>(); // Permission names assigned to this role

    @Field("is_system")
    @Builder.Default
    private boolean isSystem = false;

    @Field("is_super_admin")
    @Builder.Default
    private boolean isSuperAdmin = false;

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
