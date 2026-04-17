package com.hyp.service;

import static org.springframework.data.mongodb.core.FindAndModifyOptions.options;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import com.hyp.entity.Sequence;
import jakarta.annotation.PostConstruct;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SequenceService {

    private static final String SEQUENCE_KEY_PREFIX = "seq:";

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    MongoOperations mongoOperations;

    @PostConstruct
    public void initSequences() {
        try {
            List<Sequence> sequences = mongoOperations.findAll(Sequence.class);
            for (Sequence seq : sequences) {
                String key = SEQUENCE_KEY_PREFIX + seq.getId();
                stringRedisTemplate.opsForValue().set(key, String.valueOf(seq.getSequence()));
                log.info("Synced sequence {} = {} to Redis", seq.getId(), seq.getSequence());
            }
        } catch (Exception e) {
            log.warn("Failed to sync sequences to Redis on startup, will use MongoDB fallback", e);
        }
    }

    public String generateSequence(String sequenceName) {
        try {
            String key = SEQUENCE_KEY_PREFIX + sequenceName;
            Long nextValue = stringRedisTemplate.opsForValue().increment(key);
            syncToMongo(sequenceName, nextValue);
            return String.valueOf(nextValue);
        } catch (Exception e) {
            log.warn("Redis sequence failed for {}, falling back to MongoDB", sequenceName, e);
            return generateFromMongo(sequenceName);
        }
    }

    private static final int SYNC_MAX_RETRIES = 2;

    @Async
    void syncToMongo(String sequenceName, long value) {
        Exception lastException = null;
        for (int attempt = 1; attempt <= SYNC_MAX_RETRIES; attempt++) {
            try {
                mongoOperations.findAndModify(
                        query(where("_id").is(sequenceName)),
                        new Update().max("sequence", value),
                        options().upsert(true),
                        Sequence.class);
                return; // success
            } catch (Exception e) {
                lastException = e;
                log.warn(
                        "syncToMongo attempt {}/{} failed for sequence {}: {}",
                        attempt,
                        SYNC_MAX_RETRIES,
                        sequenceName,
                        e.getMessage());
            }
        }
        // All retries exhausted — log as error so alerting/monitoring picks it up.
        // Redis and MongoDB are now out of sync; on restart, MongoDB value will be loaded
        // which may cause a gap (not duplicate) in sequence numbers.
        log.error(
                "SEQUENCE SYNC FAILED after {} attempts for '{}' value={}. "
                        + "MongoDB sequence may be stale after restart.",
                SYNC_MAX_RETRIES,
                sequenceName,
                value,
                lastException);
    }

    private String generateFromMongo(String sequenceName) {
        Sequence sequence = mongoOperations.findAndModify(
                query(where("_id").is(sequenceName)),
                new Update().inc("sequence", 1),
                options().returnNew(true).upsert(true),
                Sequence.class);
        return String.valueOf(sequence.getSequence());
    }
}
