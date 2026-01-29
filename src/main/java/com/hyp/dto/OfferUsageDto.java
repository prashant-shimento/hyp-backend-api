package com.hyp.dto;

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
public class OfferUsageDto extends BaseDto {

    private String customerId;
    private String offerCode;
    private Double offerAmount;
    private String partnerId;
    private Integer usageCount;
}
