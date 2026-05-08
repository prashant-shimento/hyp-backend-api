package com.hyp.delivery;

import com.hyp.enums.DeliveryLifecycleEvent;
import com.hyp.enums.DeliveryPartner;
import com.hyp.service.RedisService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Tracks delivery partner health per restaurant using a Redis-backed rolling window.
 * Evaluates partner reliability, applies automatic blocks on repeated failures,
 * and provides routing signals to DeliveryOrchestrator.
 *
 * Redis key: delivery:partner:health:{restaurantId}:{partner}
 * TTL: 2 hours (rolling window — counters reset naturally on expiry)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryPartnerEvaluator {

    private static final String HEALTH_KEY = "delivery:partner:health:%s:%s";
    private static final long WINDOW_TTL_SECONDS = 7200; // 2 hours

    private static final int BLOCK_ON_CONSECUTIVE_FAILURES = 3;
    private static final int BLOCK_SHORT_MINUTES = 30;
    private static final int BLOCK_LONG_MINUTES = 60;
    private static final int MIN_ATTEMPTS_FOR_RATE_BLOCK = 5;
    private static final double POOR_SUCCESS_RATE_THRESHOLD = 0.50;

    private final RedisService redisService;
    private final PartnerScoreTracker scoreTracker;

    // ─── Read ────────────────────────────────────────────────────────────────

    public PartnerHealthState getHealth(DeliveryPartner partner, String restaurantId) {
        String key = healthKey(partner, restaurantId);
        return redisService
                .getRedisJsonData(key, PartnerHealthState.class)
                .orElseGet(() -> emptyState(partner, restaurantId));
    }

    public boolean isBlocked(DeliveryPartner partner, String restaurantId) {
        return getHealth(partner, restaurantId).isBlocked();
    }

    // ─── Record outcomes ─────────────────────────────────────────────────────

    /**
     * Called after every meaningful delivery event. Updates counters and
     * re-evaluates block conditions.
     */
    public void recordOutcome(
            DeliveryPartner partner, String restaurantId, DeliveryLifecycleEvent event, boolean success) {
        recordOutcome(partner, restaurantId, null, event, success, null);
    }

    public void recordOutcome(
            DeliveryPartner partner,
            String restaurantId,
            String deliveryId,
            DeliveryLifecycleEvent event,
            boolean success) {
        recordOutcome(partner, restaurantId, deliveryId, event, success, null);
    }

    public void recordOutcome(
            DeliveryPartner partner,
            String restaurantId,
            String deliveryId,
            DeliveryLifecycleEvent event,
            boolean success,
            Long durationMs) {
        PartnerHealthState state = getHealth(partner, restaurantId);

        switch (event) {
            case ORDER_CREATED -> {
                if (success) {
                    state.setConsecutiveFailures(0);
                } else {
                    state.setConsecutiveFailures(state.getConsecutiveFailures() + 1);
                    state.setTotalAttempts(state.getTotalAttempts() + 1);
                }
            }
            case RIDER_ASSIGNED -> {
                state.setTotalAttempts(state.getTotalAttempts() + 1);
                state.setSuccessfulAssignments(state.getSuccessfulAssignments() + 1);
                state.setConsecutiveFailures(0);
            }
            case ASSIGNMENT_TIMEOUT -> {
                state.setTotalAttempts(state.getTotalAttempts() + 1);
                state.setSlaBreachCount(state.getSlaBreachCount() + 1);
                state.setConsecutiveFailures(state.getConsecutiveFailures() + 1);
            }
            case PICKUP_TIMEOUT, DELIVERY_TIMEOUT -> {
                state.setSlaBreachCount(state.getSlaBreachCount() + 1);
            }
            case RIDER_DELIVERED -> {
                state.setConsecutiveFailures(0);
            }
            case API_FAILURE -> {
                state.setConsecutiveFailures(state.getConsecutiveFailures() + 1);
            }
            default -> {
                /* CANCELLED, PROVIDER_SWITCHED, etc. — no counter update */
            }
        }

        evaluateBlockConditions(state, partner, restaurantId);
        persist(state, partner, restaurantId);
        scoreTracker.track(partner, restaurantId, deliveryId, event, state, durationMs);
    }

    // ─── Manual block/unblock (ops) ──────────────────────────────────────────

    public void block(DeliveryPartner partner, String restaurantId, int minutes, String reason) {
        PartnerHealthState state = getHealth(partner, restaurantId);
        state.setBlockedUntil(LocalDateTime.now().plusMinutes(minutes).toString());
        persist(state, partner, restaurantId);
        log.warn(
                "Partner {} manually blocked for restaurant {} for {} min. Reason: {}",
                partner,
                restaurantId,
                minutes,
                reason);
    }

    public void unblock(DeliveryPartner partner, String restaurantId) {
        PartnerHealthState state = getHealth(partner, restaurantId);
        state.setBlockedUntil(null);
        persist(state, partner, restaurantId);
        log.info("Partner {} unblocked for restaurant {}", partner, restaurantId);
    }

    /** Clears all counters and removes any block. */
    public void reset(DeliveryPartner partner, String restaurantId) {
        persist(emptyState(partner, restaurantId), partner, restaurantId);
        log.info("Partner {} health state reset for restaurant {}", partner, restaurantId);
    }

    // ─── Internal ────────────────────────────────────────────────────────────

    private void evaluateBlockConditions(PartnerHealthState state, DeliveryPartner partner, String restaurantId) {
        if (state.isBlocked()) return; // already blocked, don't re-evaluate

        if (state.getConsecutiveFailures() >= BLOCK_ON_CONSECUTIVE_FAILURES) {
            state.setBlockedUntil(
                    LocalDateTime.now().plusMinutes(BLOCK_SHORT_MINUTES).toString());
            log.warn(
                    "Partner {} auto-blocked for {} min — {} consecutive failures for restaurant {}",
                    partner,
                    BLOCK_SHORT_MINUTES,
                    state.getConsecutiveFailures(),
                    restaurantId);
            return;
        }

        if (state.getTotalAttempts() >= MIN_ATTEMPTS_FOR_RATE_BLOCK
                && state.successRate() < POOR_SUCCESS_RATE_THRESHOLD) {
            state.setBlockedUntil(
                    LocalDateTime.now().plusMinutes(BLOCK_LONG_MINUTES).toString());
            log.warn(
                    "Partner {} auto-blocked for {} min — success rate {:.0f}% for restaurant {}",
                    partner, BLOCK_LONG_MINUTES, state.successRate() * 100, restaurantId);
        }
    }

    private void persist(PartnerHealthState state, DeliveryPartner partner, String restaurantId) {
        String key = healthKey(partner, restaurantId);
        redisService.setRedisJsonData(key, state, WINDOW_TTL_SECONDS);
    }

    private String healthKey(DeliveryPartner partner, String restaurantId) {
        return String.format(HEALTH_KEY, restaurantId, partner.name());
    }

    private PartnerHealthState emptyState(DeliveryPartner partner, String restaurantId) {
        return PartnerHealthState.builder()
                .partner(partner)
                .restaurantId(restaurantId)
                .windowStart(LocalDateTime.now().toString())
                .build();
    }
}
