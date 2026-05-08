package com.hyp.model;

import com.hyp.enums.DeliveryPartner;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryEstimate {

    private DeliveryPartner provider;
    private boolean available;
    private Double price;
    private Double distanceKm;
    private Integer etaMinutes;

    /**
     * Raw provider response for downstream use.
     * Pidge: DeliveryQuote.DeliveryNetworks (contains network token for fulfillment).
     * Adloggs: AdloggsServiceAvailabilityResponse.
     */
    private Object rawData;
}
