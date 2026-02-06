package com.hyp.event.redis.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyp.event.Event;
import com.hyp.event.EventHandler;
import com.hyp.event.EventTopic;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.StreamEntryID;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.params.XReadGroupParams;
import redis.clients.jedis.resps.StreamEntry;

@Slf4j
@AllArgsConstructor
public abstract class RedisStreamConsumer {

    private final JedisPooled jedis;
    private final ObjectMapper mapper;

    protected void consume(EventTopic topic, String consumerName, EventHandler<?> handler) {
        Map<String, StreamEntryID> streams = Map.of(topic.topicName(), StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY);

        XReadGroupParams params = XReadGroupParams.xReadGroupParams().count(10).block(5000);

        long backoffMs = 1000;
        final long maxBackoffMs = 30_000;

        while (!Thread.currentThread().isInterrupted()) {
            try {
                var results = jedis.xreadGroup(topic.consumerGroup(), consumerName, params, streams);

                // Reset backoff on success
                backoffMs = 1000;

                if (results == null || results.isEmpty()) {
                    continue;
                }

                for (var res : results) {
                    for (StreamEntry entry : res.getValue()) {
                        processEntry(topic, entry, handler);
                    }
                }

            } catch (Exception e) {
                log.error("Redis Stream consumer error, backing off {} ms", backoffMs, e);

                try {
                    Thread.sleep(backoffMs + jitter(200));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }

                backoffMs = Math.min(backoffMs * 2, maxBackoffMs);
            }
        }

        log.info("Redis Stream consumer stopped cleanly for {}", consumerName);
    }

    private void processEntry(EventTopic topic, StreamEntry entry, EventHandler<?> handler) {
        String streamId = entry.getID().toString();

        try {
            String payload = entry.getFields().get("payload");
            Event event = mapper.readValue(payload, handler.payloadType());

            handleEvent(handler, event);

            jedis.xack(topic.topicName(), topic.consumerGroup(), entry.getID());

        } catch (Exception e) {
            log.error("Failed processing message {}", streamId, e);
            // NO ACK → retry via PEL
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends Event> void handleEvent(EventHandler<T> handler, Event event) throws Exception {
        handler.handle((T) event);
    }

    private long jitter(int maxMs) {
        return ThreadLocalRandom.current().nextLong(maxMs);
    }

    protected void ensureStreamAndGroup(EventTopic topic) {
        try {
            jedis.xgroupCreate(
                    topic.topicName(), topic.consumerGroup(), StreamEntryID.XGROUP_LAST_ENTRY, true // MKSTREAM
                    );
            log.info("Ensured Redis stream and group stream={} group={}", topic.topicName(), topic.consumerGroup());
        } catch (JedisDataException e) {
            if (e.getMessage() != null && e.getMessage().contains("BUSYGROUP")) {
                log.debug("Consumer group already exists stream={} group={}", topic.topicName(), topic.consumerGroup());
            } else {
                throw e;
            }
        }
    }
}
