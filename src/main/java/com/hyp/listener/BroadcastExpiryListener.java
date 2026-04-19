package com.hyp.listener;

import com.hyp.service.BroadcastService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BroadcastExpiryListener implements MessageListener {

    @Autowired
    private BroadcastService broadcastService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = message.toString();
        if (!expiredKey.startsWith("broadcast:expiry:")) return;

        // key format: broadcast:expiry:{broadcastId}
        String[] parts = expiredKey.split(":");
        if (parts.length < 3) {
            log.warn("Invalid broadcast expiry key: {}", expiredKey);
            return;
        }
        String broadcastId = parts[2];
        log.info("Broadcast TTL expired broadcastId={}", broadcastId);
        try {
            broadcastService.handleExpiry(broadcastId);
        } catch (Exception e) {
            log.error("Error handling broadcast expiry broadcastId={}", broadcastId, e);
        }
    }
}
