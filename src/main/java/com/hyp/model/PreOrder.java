package com.hyp.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.concurrent.TimeUnit;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PreOrder {
    @Field("is_pre_order_enabled")
    private boolean isPreOrderEnabled;

    @Field("min_duration")
    private int minDuration;

    @Field("max_duration")
    private int maxDuration;

    @Field("time_unit")
    private String timeUnit;
}
