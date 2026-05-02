package com.hyp.entity;

import com.hyp.annotation.GenerateId;
import com.hyp.service.RestaurantScoped;
import jakarta.persistence.MappedSuperclass;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@MappedSuperclass
public abstract class BaseEntity implements Identifiable<String>, Serializable, RestaurantScoped {

    @Id
    @Field("id")
    @GenerateId()
    private String id;

    @Field("created_at")
    @CreatedDate
    private LocalDateTime createdAt = LocalDateTime.now();

    @Field("updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Field("restaurant_id")
    private String restaurantId;

    @Field("is_deleted")
    private boolean isDeleted = false;
}
