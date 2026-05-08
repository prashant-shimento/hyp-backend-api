package com.hyp.entity;

import com.hyp.annotation.GenerateId;
import com.hyp.enums.DeliveryPartner;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "delivery_partner_scores")
public class DeliveryPartnerScore {

    @Id
    @Field("id")
    @GenerateId
    private String id;

    @Field("restaurant_id")
    private String restaurantId;

    private DeliveryPartner partner;

    private LocalDate date; // one document per partner per restaurant per day

    // ── Current health score ──────────────────────────────────────────────────
    @Field("health_score")
    private int healthScore; // 0–100 composite

    @Field("success_rate")
    private double successRate; // successfulAssignments / totalAttempts

    @Field("sla_compliance_rate")
    private double slaComplianceRate; // (attempts - breaches) / attempts

    @Field("consecutive_failures")
    private int consecutiveFailures;

    @Field("is_blocked")
    private boolean blocked;

    // ── Daily counters ────────────────────────────────────────────────────────
    @Field("total_attempts")
    private int totalAttempts;

    @Field("successful_assignments")
    private int successfulAssignments;

    @Field("sla_breach_count")
    private int slaBreachCount;

    @Field("api_failures")
    private int apiFailures;

    @Field("total_deliveries")
    private int totalDeliveries; // RIDER_DELIVERED count

    // ── Timing averages (milliseconds) ────────────────────────────────────────
    // Stored as sum + count so averages can be recomputed accurately.
    @Field("assignment_time_total_ms")
    private long assignmentTimeTotalMs; // sum of ORDER_CREATED → RIDER_ASSIGNED durations

    @Field("assignment_time_count")
    private int assignmentTimeCount;

    @Field("pickup_time_total_ms")
    private long pickupTimeTotalMs; // sum of RIDER_ASSIGNED → RIDER_PICKED_UP durations

    @Field("pickup_time_count")
    private int pickupTimeCount;

    @Field("delivery_time_total_ms")
    private long deliveryTimeTotalMs; // sum of RIDER_PICKED_UP → RIDER_DELIVERED durations

    @Field("delivery_time_count")
    private int deliveryTimeCount;

    @Field("last_updated_at")
    private LocalDateTime lastUpdatedAt;

    // ── Computed helpers (not stored) ─────────────────────────────────────────
    public Long avgAssignmentMs() {
        return assignmentTimeCount > 0 ? assignmentTimeTotalMs / assignmentTimeCount : null;
    }

    public Long avgPickupMs() {
        return pickupTimeCount > 0 ? pickupTimeTotalMs / pickupTimeCount : null;
    }

    public Long avgDeliveryMs() {
        return deliveryTimeCount > 0 ? deliveryTimeTotalMs / deliveryTimeCount : null;
    }
}
