package com.hyp.config;

import com.hyp.listener.DeliveryListener;
import com.hyp.listener.ItemStockListener;
import com.hyp.listener.OrderListener;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import redis.clients.jedis.Connection;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisPooled;

@Slf4j
@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private Integer redisPort;

    @Value("${spring.data.redis.password}")
    private String redisPassword;

    // Connection pool settings - can be configured via application.properties
    @Value("${spring.data.redis.jedis.pool.max-active:50}")
    private int maxActive;

    @Value("${spring.data.redis.jedis.pool.max-idle:20}")
    private int maxIdle;

    @Value("${spring.data.redis.jedis.pool.min-idle:5}")
    private int minIdle;

    @Value("${spring.data.redis.jedis.pool.max-wait:5000}")
    private long maxWaitMillis;

    @Value("${spring.data.redis.timeout:5000}")
    private int timeout;

    /**
     * Main connection pool configuration for JedisPooled (primary Redis client).
     * This is the main pool used for all Redis operations.
     */
    @Bean
    public GenericObjectPoolConfig<Connection> jedisPoolConfig() {
        GenericObjectPoolConfig<Connection> poolConfig = new GenericObjectPoolConfig<>();

        // Pool size settings - this is the main pool for all operations
        poolConfig.setMaxTotal(maxActive);
        poolConfig.setMaxIdle(maxIdle);
        poolConfig.setMinIdle(minIdle);

        // Blocking behavior when pool exhausted
        poolConfig.setMaxWait(Duration.ofMillis(maxWaitMillis));
        poolConfig.setBlockWhenExhausted(true);

        // Connection validation
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(false);
        poolConfig.setTestWhileIdle(true);

        // Eviction settings for idle connections
        poolConfig.setTimeBetweenEvictionRuns(Duration.ofSeconds(30));
        poolConfig.setMinEvictableIdleDuration(Duration.ofMinutes(5));
        poolConfig.setNumTestsPerEvictionRun(3);

        // JMX monitoring
        poolConfig.setJmxEnabled(true);
        poolConfig.setJmxNamePrefix("jedis-pooled");

        log.info(
                "JedisPooled pool configured: maxActive={}, maxIdle={}, minIdle={}, maxWait={}ms",
                maxActive,
                maxIdle,
                minIdle,
                maxWaitMillis);

        return poolConfig;
    }

    /**
     * Minimal pool configuration for JedisConnectionFactory (pub/sub only).
     * This factory is only used by RedisMessageListenerContainer for key expiry events.
     */
    @Bean
    public GenericObjectPoolConfig<Connection> pubSubPoolConfig() {
        GenericObjectPoolConfig<Connection> poolConfig = new GenericObjectPoolConfig<>();

        // Minimal pool - only needed for pub/sub subscriptions
        poolConfig.setMaxTotal(4);
        poolConfig.setMaxIdle(2);
        poolConfig.setMinIdle(1);

        poolConfig.setMaxWait(Duration.ofMillis(maxWaitMillis));
        poolConfig.setBlockWhenExhausted(true);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setTimeBetweenEvictionRuns(Duration.ofSeconds(60));
        poolConfig.setMinEvictableIdleDuration(Duration.ofMinutes(10));

        poolConfig.setJmxEnabled(true);
        poolConfig.setJmxNamePrefix("pubsub-pool");

        log.info("Pub/Sub pool configured: maxActive=4, maxIdle=2, minIdle=1");

        return poolConfig;
    }

    /**
     * JedisConnectionFactory - used ONLY for pub/sub (RedisMessageListenerContainer).
     * Uses minimal pool since it only handles key expiry event subscriptions.
     */
    @Bean
    public JedisConnectionFactory jedisConnectionFactory(GenericObjectPoolConfig<Connection> pubSubPoolConfig) {
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration(redisHost, redisPort);
        redisConfig.setPassword(redisPassword);

        JedisClientConfiguration clientConfig = JedisClientConfiguration.builder()
                .usePooling()
                .poolConfig(pubSubPoolConfig)
                .and()
                .connectTimeout(Duration.ofMillis(timeout))
                .readTimeout(Duration.ofMillis(timeout))
                .build();

        JedisConnectionFactory factory = new JedisConnectionFactory(redisConfig, clientConfig);
        log.info("JedisConnectionFactory created for pub/sub with host={}, port={}", redisHost, redisPort);
        return factory;
    }

    /**
     * JedisPooled - the PRIMARY Redis client for all operations.
     * All Redis operations (get, set, json, etc.) go through this pool.
     */
    @Bean
    public JedisPooled jedisPooled(GenericObjectPoolConfig<Connection> jedisPoolConfig) {
        HostAndPort hostAndPort = new HostAndPort(redisHost, redisPort);
        JedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
                .password(redisPassword)
                .connectionTimeoutMillis(timeout)
                .socketTimeoutMillis(timeout)
                .build();

        JedisPooled jedisPooled = new JedisPooled(jedisPoolConfig, hostAndPort, clientConfig);
        log.info("JedisPooled (primary) created with host={}, port={}", redisHost, redisPort);
        return jedisPooled;
    }

    @Bean
    RedisMessageListenerContainer redisContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter orderListenerAdapter,
            MessageListenerAdapter itemStockListenerAdapter,
            MessageListenerAdapter deliveryListenerAdapter) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(orderListenerAdapter, new PatternTopic("__keyevent@0__:expired"));
        container.addMessageListener(itemStockListenerAdapter, new PatternTopic("__keyevent@0__:expired"));
        container.addMessageListener(deliveryListenerAdapter, new PatternTopic("__keyevent@0__:expired"));
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
}
