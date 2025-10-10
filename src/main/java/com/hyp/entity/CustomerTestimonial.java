package com.hyp.entity;

import com.hyp.annotation.GenerateId;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "customer_testimonial")
public class CustomerTestimonial {

    @Id
    @GenerateId(sequenceName = "customer_testimonial_sequence")
    private String id;

    @Field("partnerId")
    private String partnerId;

    @Field("restaurant_id")
    private String restaurantId;

    @Field("customerName")
    private String customerName;

    @Field("reviews")
    private String reviews;

    @Field("rating")
    private Double rating;
}
