package com.hyp.delivery;

import com.hyp.entity.DeliveryPartnerScore;
import com.hyp.enums.DeliveryLifecycleEvent;
import com.hyp.enums.DeliveryPartner;
import com.hyp.repository.DeliveryPartnerScoreRepository;
import com.hyp.service.RedisService;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Persists a daily partner score snapshot to MongoDB on every delivery status change.
 * Also tracks per-stage timing (assignment, pickup, delivery) using Redis timestamps.
 *
 * Redis key for timing:  delivery:event:ts:{deliveryId}  →  "{eventType}|{isoTimestamp}"
 * MongoDB collection:    partner_score_snapshots          →  one doc per partner/restaurant/day
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PartnerScoreTracker {

    private static final String EVENT_TS_KEY = "delivery:event:ts:%s";
    private static final long EVENT_TS_TTL_SECONDS = 86400; // 24 hours

    private final DeliveryPartnerScoreRepository repository;
    private final RedisService redisService;

    /**
     * Called after every recordOutcome() in DeliveryPartnerEvaluator.
     * durationMs must be pre-computed by the caller (synchronously at event time)
     * to avoid double-computation and async timing drift.
     */
    public void track(
            DeliveryPartner partner,
            String restaurantId,
            String deliveryId,
            DeliveryLifecycleEvent event,
            PartnerHealthState state,
            Long durationMs) {
        CompletableFuture.runAsync(() -> {
            try {
                upsertSnapshot(partner, restaurantId, event, state, durationMs);
            } catch (Exception e) {
                log.error(
                        "Failed to update partner score snapshot partner={} restaurant={} deliveryId={}",
                        partner,
                        restaurantId,
                        deliveryId,
                        e);
            }
        });
    }

    // ─── Timing ──────────────────────────────────────────────────────────────

    /**
     * Reads the previous event timestamp from Redis, computes durationMs,
     * then stores the current event timestamp for the next stage.
     * Returns null if no previous timestamp exists (first event).
     */
    public Long computeAndUpdateTiming(String deliveryId, DeliveryLifecycleEvent event) {
        if (deliveryId == null) return null;

        String key = String.format(EVENT_TS_KEY, deliveryId);
        String now = LocalDateTime.now().toString();
        Long durationMs = null;

        String previous = redisService.getRedisData(key).orElse(null);
        if (previous != null) {
            try {
                String[] parts = previous.split("\\|");
                LocalDateTime prevTime = LocalDateTime.parse(parts[1]);
                durationMs = Duration.between(prevTime, LocalDateTime.now()).toMillis();
            } catch (Exception e) {
                log.debug("Could not parse previous event timestamp deliveryId={}", deliveryId);
            }
        }

        redisService.setRedisStringDataSync(key, event.name() + "|" + now, EVENT_TS_TTL_SECONDS);
        return durationMs;
    }

    // ─── Snapshot upsert ─────────────────────────────────────────────────────

    private void upsertSnapshot(
            DeliveryPartner partner,
            String restaurantId,
            DeliveryLifecycleEvent event,
            PartnerHealthState state,
            Long durationMs) {
        LocalDate today = LocalDate.now();

        DeliveryPartnerScore snapshot = repository
                .findByRestaurantIdAndPartnerAndDate(restaurantId, partner, today)
                .orElseGet(() -> DeliveryPartnerScore.builder()
                        .restaurantId(restaurantId)
                        .partner(partner)
                        .date(today)
                        .build());

        // Always refresh score fields from current health state
        snapshot.setHealthScore(state.healthScore());
        snapshot.setSuccessRate(state.successRate());
        double slaCompliance = state.getTotalAttempts() == 0
                ? 1.0
                : (double) (state.getTotalAttempts() - state.getSlaBreachCount()) / state.getTotalAttempts();
        snapshot.setSlaComplianceRate(slaCompliance);
        snapshot.setConsecutiveFailures(state.getConsecutiveFailures());
        snapshot.setBlocked(state.isBlocked());
        snapshot.setTotalAttempts(state.getTotalAttempts());
        snapshot.setSuccessfulAssignments(state.getSuccessfulAssignments());
        snapshot.setSlaBreachCount(state.getSlaBreachCount());
        snapshot.setLastUpdatedAt(LocalDateTime.now());

        // Increment event-specific counters
        switch (event) {
            case API_FAILURE -> snapshot.setApiFailures(snapshot.getApiFailures() + 1);
            case RIDER_DELIVERED -> snapshot.setTotalDeliveries(snapshot.getTotalDeliveries() + 1);
            default -> {}
        }

        // Accumulate timing for each stage
        if (durationMs != null && durationMs > 0) {
            switch (event) {
                case RIDER_ASSIGNED -> {
                    snapshot.setAssignmentTimeTotalMs(snapshot.getAssignmentTimeTotalMs() + durationMs);
                    snapshot.setAssignmentTimeCount(snapshot.getAssignmentTimeCount() + 1);
                }
                case RIDER_PICKED_UP -> {
                    snapshot.setPickupTimeTotalMs(snapshot.getPickupTimeTotalMs() + durationMs);
                    snapshot.setPickupTimeCount(snapshot.getPickupTimeCount() + 1);
                }
                case RIDER_DELIVERED -> {
                    snapshot.setDeliveryTimeTotalMs(snapshot.getDeliveryTimeTotalMs() + durationMs);
                    snapshot.setDeliveryTimeCount(snapshot.getDeliveryTimeCount() + 1);
                }
                default -> {}
            }
        }

        repository.save(snapshot);
        log.debug(
                "Score snapshot updated partner={} restaurant={} score={} event={}",
                partner,
                restaurantId,
                snapshot.getHealthScore(),
                event);
    }
}
