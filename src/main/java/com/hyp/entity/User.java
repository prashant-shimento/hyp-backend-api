package com.hyp.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.hyp.annotation.GenerateId;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Document(collection = "users")
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(Include.NON_NULL)
public class User {

    @Id
    @GenerateId(sequenceName = "user_sequence")
    private String id;

    @Field("name")
    private String name;

    @Field("mobile")
    private String mobile;

    @Field("email")
    @Indexed(unique = true)
    private String email;

    @Field("password")
    private String password;

    @Field("created_at")
    @CreatedDate
    private LocalDateTime createdAt = LocalDateTime.now();

    @Field("updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;

    private Boolean active;

    @Field("restaurant_ids")
    private Set<String> restaurantIds = new HashSet<>();

    private String partnerId;

    @Field("role")
    private String role; // RESTAURANT_USER, RESTAURANT_ADMIN, PLATFORM_USER, PLATFORM_ADMIN
}
