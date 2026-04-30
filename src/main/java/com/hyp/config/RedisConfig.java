package com.hyp.config;

import com.hyp.listener.BroadcastExpiryListener;
import com.hyp.listener.DeliveryListener;
import com.hyp.listener.ItemStockListener;
import com.hyp.listener.OrderListener;
import com.hyp.listener.SecurityCacheInvalidationListener;
import com.hyp.security.service.SecurityCacheService;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Slf4j
@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private Integer redisPort;

    @Value("${spring.data.redis.password}")
    private String redisPassword;

    @Value("${spring.data.redis.lettuce.pool.max-active:50}")
    private int maxActive;

    @Value("${spring.data.redis.lettuce.pool.max-idle:20}")
    private int maxIdle;

    @Value("${spring.data.redis.lettuce.pool.min-idle:5}")
    private int minIdle;

    @Value("${spring.data.redis.lettuce.pool.max-wait:5000ms}")
    private Duration maxWait;

    @Value("${spring.data.redis.timeout:5000}")
    private int timeout;

    /**
     * Lettuce connection factory with connection pooling.
     * Single factory handles both pub/sub and all Redis operations.
     * Lettuce uses a single, thread-safe connection by default and pools when configured.
     */
    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration(redisHost, redisPort);
        redisConfig.setPassword(redisPassword);

        GenericObjectPoolConfig<?> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(maxActive);
        poolConfig.setMaxIdle(maxIdle);
        poolConfig.setMinIdle(minIdle);
        poolConfig.setMaxWait(maxWait);
        poolConfig.setBlockWhenExhausted(true);

        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(false);
        poolConfig.setTestWhileIdle(true);

        poolConfig.setTimeBetweenEvictionRuns(Duration.ofSeconds(30));
        poolConfig.setMinEvictableIdleDuration(Duration.ofMinutes(5));
        poolConfig.setNumTestsPerEvictionRun(3);

        // commandTimeout must exceed the longest blocking operation (e.g. XREADGROUP block 5s)
        // otherwise Lettuce kills the command before the block returns
        long commandTimeoutMs = Math.max(timeout, 15_000);

        LettucePoolingClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
                .poolConfig(poolConfig)
                .commandTimeout(Duration.ofMillis(commandTimeoutMs))
                .build();

        log.info(
                "Lettuce pool configured: maxActive={}, maxIdle={}, minIdle={}, maxWait={}",
                maxActive,
                maxIdle,
                minIdle,
                maxWait);

        return new LettuceConnectionFactory(redisConfig, clientConfig);
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    @Bean
    RedisMessageListenerContainer redisContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter orderListenerAdapter,
            MessageListenerAdapter itemStockListenerAdapter,
            MessageListenerAdapter deliveryListenerAdapter,
            MessageListenerAdapter broadcastExpiryListenerAdapter,
            MessageListenerAdapter securityCacheListenerAdapter) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(orderListenerAdapter, new PatternTopic("__keyevent@0__:expired"));
        container.addMessageListener(itemStockListenerAdapter, new PatternTopic("__keyevent@0__:expired"));
        container.addMessageListener(deliveryListenerAdapter, new PatternTopic("__keyevent@0__:expired"));
        container.addMessageListener(broadcastExpiryListenerAdapter, new PatternTopic("__keyevent@0__:expired"));
        container.addMessageListener(
                securityCacheListenerAdapter, new ChannelTopic(SecurityCacheService.CACHE_INVALIDATION_CHANNEL));
        return container;
    }

    @Bean
    MessageListenerAdapter orderListenerAdapter(OrderListener orderListener) {
        return new MessageListenerAdapter(orderListener);
    }

    @Bean
    MessageListenerAdapter itemStockListenerAdapter(ItemStockListener itemStockListener) {
        return new MessageListenerAdapter(itemStockListener);
    }

    @Bean
    MessageListenerAdapter deliveryListenerAdapter(DeliveryListener deliveryListener) {
        return new MessageListenerAdapter(deliveryListener);
    }

    @Bean
    MessageListenerAdapter broadcastExpiryListenerAdapter(BroadcastExpiryListener broadcastExpiryListener) {
        return new MessageListenerAdapter(broadcastExpiryListener);
    }

    @Bean
    MessageListenerAdapter securityCacheListenerAdapter(SecurityCacheInvalidationListener listener) {
        return new MessageListenerAdapter(listener);
    }
}
