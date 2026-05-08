package com.hyp.delivery;

import com.hyp.enums.DeliveryPartner;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerHealthState {

    private DeliveryPartner partner;
    private String restaurantId;

    // Rolling window counters (reset when window expires)
    private int consecutiveFailures;
    private int totalAttempts;
    private int successfulAssignments;
    private int slaBreachCount;

    // ISO string — easier to JSON-serialize than LocalDateTime with ObjectMapper
    private String blockedUntil; // null = not blocked
    private String windowStart; // when current rolling window started

    public boolean isBlocked() {
        if (blockedUntil == null || blockedUntil.isBlank()) return false;
        try {
            return LocalDateTime.now().isBefore(LocalDateTime.parse(blockedUntil));
        } catch (Exception e) {
            return false;
        }
    }

    /** Returns assignment success rate. Defaults to 1.0 (healthy) when no data. */
    public double successRate() {
        if (totalAttempts == 0) return 1.0;
        return (double) successfulAssignments / totalAttempts;
    }

    /** 0–100 composite score. Higher = healthier. */
    public int healthScore() {
        if (isBlocked()) return 0;
        double slaCompliance = totalAttempts == 0 ? 1.0 : (double) (totalAttempts - slaBreachCount) / totalAttempts;
        return (int) Math.round((successRate() * 60) + (slaCompliance * 40));
    }
}
