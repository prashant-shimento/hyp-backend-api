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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
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
     * Shared connection pool configuration for all Redis clients.
     */
    @Bean
    public GenericObjectPoolConfig<Connection> jedisPoolConfig() {
        GenericObjectPoolConfig<Connection> poolConfig = new GenericObjectPoolConfig<>();

        // Pool size settings
        poolConfig.setMaxTotal(maxActive); // Maximum connections in pool
        poolConfig.setMaxIdle(maxIdle); // Maximum idle connections
        poolConfig.setMinIdle(minIdle); // Minimum idle connections

        // Blocking behavior when pool exhausted
        poolConfig.setMaxWait(Duration.ofMillis(maxWaitMillis));
        poolConfig.setBlockWhenExhausted(true);

        // Connection validation
        poolConfig.setTestOnBorrow(true); // Validate before borrowing
        poolConfig.setTestOnReturn(false); // Don't validate on return
        poolConfig.setTestWhileIdle(true); // Validate idle connections

        // Eviction settings for idle connections
        poolConfig.setTimeBetweenEvictionRuns(Duration.ofSeconds(30));
        poolConfig.setMinEvictableIdleDuration(Duration.ofMinutes(5));
        poolConfig.setNumTestsPerEvictionRun(3);

        // JMX monitoring
        poolConfig.setJmxEnabled(true);
        poolConfig.setJmxNamePrefix("redis-pool");

        log.info(
                "Redis pool configured: maxActive={}, maxIdle={}, minIdle={}, maxWait={}ms",
                maxActive,
                maxIdle,
                minIdle,
                maxWaitMillis);

        return poolConfig;
    }

    @Bean
    public JedisConnectionFactory jedisConnectionFactory(GenericObjectPoolConfig<Connection> poolConfig) {
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration(redisHost, redisPort);
        redisConfig.setPassword(redisPassword);

        JedisClientConfiguration clientConfig = JedisClientConfiguration.builder()
                .usePooling()
                .poolConfig(poolConfig)
                .and()
                .connectTimeout(Duration.ofMillis(timeout))
                .readTimeout(Duration.ofMillis(timeout))
                .build();

        JedisConnectionFactory factory = new JedisConnectionFactory(redisConfig, clientConfig);
        log.info("JedisConnectionFactory created with host={}, port={}", redisHost, redisPort);
        return factory;
    }

    @Bean
    RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    /**
     * JedisPooled for direct Jedis operations (JSON module, etc.)
     * Uses separate pool but with same configuration.
     */
    @Bean
    public JedisPooled jedisPooled(GenericObjectPoolConfig<Connection> poolConfig) {
        HostAndPort hostAndPort = new HostAndPort(redisHost, redisPort);
        JedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
                .password(redisPassword)
                .connectionTimeoutMillis(timeout)
                .socketTimeoutMillis(timeout)
                .build();

        JedisPooled jedisPooled = new JedisPooled(poolConfig, hostAndPort, clientConfig);
        log.info("JedisPooled created with host={}, port={}", redisHost, redisPort);
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
