package com.hyp.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.hyp.annotation.GenerateId;
import com.hyp.model.Location;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "address")
@JsonInclude(Include.NON_NULL)
public class Address {

    @Id
    @GenerateId(sequenceName = "address_sequence")
    private String id;

    @Field("address_type")
    private String addressType;

    @Field("address_one")
    private String addressOne;

    @Field("address_two")
    private String addressTwo;

    @Field("landmark")
    private String landmark;

    @Field("city")
    private String city;

    @Field("state")
    private String state;

    @Field("country")
    private String country;

    @Field("pincode")
    private String pincode;

    @Field("customer_id")
    private String customerId;

    private Location location;

    @Field("created_at")
    @CreatedDate
    private LocalDateTime createdAt = LocalDateTime.now();

    ;

    @Field("updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Field("restaurant_id")
    private String restaurantId;
}
