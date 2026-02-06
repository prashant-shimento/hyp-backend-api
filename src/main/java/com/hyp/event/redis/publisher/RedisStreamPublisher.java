package com.hyp.event.redis.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyp.event.Event;
import com.hyp.event.EventPublisher;
import com.hyp.event.EventTopic;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.StreamEntryID;

@Component
@Slf4j
@AllArgsConstructor
public class RedisStreamPublisher implements EventPublisher {

    private final JedisPooled jedis;
    private final ObjectMapper mapper;

    @Override
    public String publish(EventTopic topic, Event event) {
        try {
            ensureGroup(topic);

            Map<String, String> fields = Map.of(
                    "eventId", event.getEventId(),
                    "type", event.getType(),
                    "aggregateId", event.getAggregateId(),
                    "createdAt", String.valueOf(event.getCreatedAt()),
                    "payload", mapper.writeValueAsString(event));

            StreamEntryID id = jedis.xadd(topic.topicName(), StreamEntryID.NEW_ENTRY, fields);

            log.info("Published event type={} topic={} id={}", event.getType(), topic.topicName(), id);

            return id.toString();

        } catch (Exception e) {
            log.error("Failed to publish event {}", event.getType(), e);
            return null;
        }
    }

    private void ensureGroup(EventTopic topic) {
        try {
            jedis.xgroupCreate(topic.topicName(), topic.consumerGroup(), StreamEntryID.XGROUP_LAST_ENTRY, true);
        } catch (Exception e) {
            if (!e.getMessage().contains("BUSYGROUP")) {
                throw e;
            }
        }
    }
}
