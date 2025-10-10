package com.hyp.dto;

import com.hyp.enums.OfferStatusType;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class OfferDto {

    private String id;

    private String title;

    private String imageUrl;

    private String description;

    private String couponCode;

    private String discountType;

    private OfferStatusType offerStatusType;

    private double discountValue;

    private double maxDiscountValue;

    private double maxDiscount;

    private double minimumOrderValue;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private int redemptionLimit;

    private int redemptionsCount;
}
