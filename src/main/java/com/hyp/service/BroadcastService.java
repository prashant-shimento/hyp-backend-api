package com.hyp.service;

import com.hyp.constants.Constants;
import com.hyp.entity.Broadcast;
import com.hyp.enums.BroadcastScope;
import com.hyp.enums.BroadcastType;
import com.hyp.model.BroadcastMessage;
import com.hyp.repository.BroadcastRepository;
import com.hyp.request.BroadcastRequest;
import com.hyp.util.CommonUtils;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class BroadcastService {

    @Autowired
    private SimpMessagingTemplate messageTemplate;

    @Autowired
    private BroadcastRepository broadcastRepository;

    @Autowired
    private RedisService redisService;

    /** Admin-triggered broadcast. Persists and pushes to WebSocket topic. */
    public Broadcast send(BroadcastRequest request) {
        Broadcast broadcast = new Broadcast();
        broadcast.setId(CommonUtils.genId());
        broadcast.setScope(request.getScope());
        broadcast.setReceiverId(request.getReceiverId());
        broadcast.setType(request.getType());
        broadcast.setMessage(request.getMessage());
        broadcast.setActive(true);
        broadcast.setTriggeredBy("ADMIN");
        if (request.getTtlMinutes() != null) {
            broadcast.setExpiresAt(LocalDateTime.now().plusMinutes(request.getTtlMinutes()));
        }
        broadcastRepository.save(broadcast);

        if (request.getTtlMinutes() != null) {
            redisService.setRedisStringDataSync(
                    String.format(Constants.REDIS_BROADCAST_EXPIRY, broadcast.getId()),
                    broadcast.getId(),
                    Duration.ofMinutes(request.getTtlMinutes()).toSeconds());
        }

        push(
                request.getScope(),
                request.getReceiverId(),
                BroadcastMessage.builder()
                        .broadcastId(broadcast.getId())
                        .type(broadcast.getType())
                        .status("ACTIVE")
                        .message(broadcast.getMessage())
                        .triggeredBy("ADMIN")
                        .expiresAt(broadcast.getExpiresAt())
                        .build());
        log.info(
                "Broadcast sent scope={} receiverId={} type={}",
                request.getScope(),
                request.getReceiverId(),
                request.getType());
        return broadcast;
    }

    /** Explicit clear by broadcastId. */
    public void clear(String broadcastId) {
        Broadcast broadcast = broadcastRepository
                .findById(broadcastId)
                .orElseThrow(() -> new IllegalArgumentException("Broadcast not found: " + broadcastId));
        if (!broadcast.isActive()) {
            log.warn("Broadcast already cleared broadcastId={}", broadcastId);
            return;
        }
        broadcast.setActive(false);
        broadcast.setClearedAt(LocalDateTime.now());
        broadcastRepository.save(broadcast);
        redisService.removeRedisData(String.format(Constants.REDIS_BROADCAST_EXPIRY, broadcastId));
        push(
                broadcast.getScope(),
                broadcast.getReceiverId(),
                BroadcastMessage.builder()
                        .broadcastId(broadcastId)
                        .type(broadcast.getType())
                        .status("CLEARED")
                        .build());
        log.info("Broadcast cleared broadcastId={}", broadcastId);
    }

    /** Called by BroadcastExpiryListener when the Redis TTL key fires. */
    public void handleExpiry(String broadcastId) {
        broadcastRepository.findById(broadcastId).ifPresent(broadcast -> {
            if (!broadcast.isActive()) return;
            broadcast.setActive(false);
            broadcast.setClearedAt(LocalDateTime.now());
            broadcastRepository.save(broadcast);
            push(
                    broadcast.getScope(),
                    broadcast.getReceiverId(),
                    BroadcastMessage.builder()
                            .broadcastId(broadcastId)
                            .type(broadcast.getType())
                            .status("CLEARED")
                            .build());
            log.info("Broadcast auto-expired broadcastId={}", broadcastId);
        });
    }

    /** System-triggered push (e.g. RiderAvailabilityMonitor). No persistence. */
    public void systemBroadcast(String restaurantId, BroadcastType type, String message) {
        push(
                BroadcastScope.RESTAURANT,
                restaurantId,
                BroadcastMessage.builder()
                        .type(type)
                        .status("ACTIVE")
                        .message(message)
                        .triggeredBy("SYSTEM")
                        .build());
    }

    public List<Broadcast> getActive(BroadcastScope scope, String targetId) {
        return broadcastRepository.findByScopeAndReceiverIdAndActiveTrue(scope, targetId);
    }

    private void push(BroadcastScope scope, String targetId, BroadcastMessage message) {
        String topic =
                switch (scope) {
                    case PARTNER -> "/topic/broadcast/partner/" + targetId;
                    case RESTAURANT -> "/topic/broadcast/restaurant/" + targetId;
                    case CUSTOMER -> "/topic/broadcast/customer/" + targetId;
                };
        messageTemplate.convertAndSend(topic, message);
    }
}
