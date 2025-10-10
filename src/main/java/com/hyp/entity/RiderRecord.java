package com.hyp.entity;

import com.hyp.annotation.GenerateId;
import java.util.ArrayList;
import java.util.List;
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
@Document(collection = "rider_records")
public class RiderRecord {

    @Id
    @Field("id")
    @GenerateId(sequenceName = "rider_sequence")
    private String id;

    @Field("rider_name")
    private String riderName;

    @Field("rider_phone_number")
    private String riderContact;

    @Field("fraud_count")
    private Integer fraudCount;

    @Field("channels")
    private List<String> channels = new ArrayList<>();
}
