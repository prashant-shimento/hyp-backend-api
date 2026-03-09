package com.hyp.entity;

import com.hyp.annotation.GenerateId;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "affiliates")
public class Affiliate {

    @Id
    @Field("id")
    @GenerateId(sequenceName = "affiliate_sequence")
    private String id;

    @Field("name")
    private String name;

    @Field("domain")
    private String domain;

    @Field("active")
    private Boolean active;

    @Field("color_theme")
    private String colorTheme;

    @Field("logo_url")
    private String logoUrl;

    @Field("serviceable")
    private Boolean serviceable;

    @Field("alt_contact")
    private String altContact;

    @Field("launched")
    private String launched;

    @Field("address")
    private String address;

    @Field("map_url")
    private String mapUrl;

    @Field("email")
    private String email;

    @Field("google_analytics")
    private String googleAnalytics;

    @Field("created_at")
    @CreatedDate
    private LocalDateTime createdAt = LocalDateTime.now();

    @Field("updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
