package com.hyp.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.hyp.annotation.GenerateId;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Document(collection = "access_policies")
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(Include.NON_NULL)
@Builder
@CompoundIndexes({@CompoundIndex(name = "resource_operation_idx", def = "{'resource': 1, 'operation': 1}")})
public class AccessPolicy {

    @Id
    @Field("id")
    @GenerateId()
    private String id;

    @Field("resource")
    private String resource; // "/customer", "/order/**", "/menu/{id}"

    @Field("operation")
    private String operation; // "GET", "POST", "PATCH", "DELETE", "*"

    @Field("permissions")
    private List<String> permissions; // Required permissions (any match grants access)

    @Field("is_public")
    @Builder.Default
    private boolean isPublic = false; // No auth required

    @Field("restaurant_scoped")
    @Builder.Default
    private boolean restaurantScoped = false; // Enforce tenant isolation

    @Field("owner_only")
    @Builder.Default
    private boolean ownerOnly = false; // User can only access own resources

    @Field("webhook_auth")
    @Builder.Default
    private boolean webhookAuth = false; // Requires API key + signature

    @Field("priority")
    @Builder.Default
    private int priority = 50; // Higher = evaluated first (for conflicts)

    @Field("active")
    @Builder.Default
    private boolean active = true;

    @Field("description")
    private String description;

    @Field("created_at")
    @CreatedDate
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Field("updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
