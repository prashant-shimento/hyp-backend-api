package com.hyp.dto;

import com.hyp.enums.OfferType;
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
public class OfferDto extends BaseDto {

    private String offerCode;
    private OfferType offerType;
    private Double discountValue;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    private String maximumRedemptionLimit;
    private Boolean isActive;

    private String partnerId;
    private String notes;
}
