package com.hyp.config;

import com.hyp.listener.DeliveryListener;
import com.hyp.listener.ItemStockListener;
import com.hyp.listener.OrderListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
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

    @Value("${spring.data.redis.timeout:5000}")
    private int timeout;

    @Bean
    public JedisPooled jedisPooled() {
        return new JedisPooled(
                new HostAndPort(redisHost, redisPort),
                DefaultJedisClientConfig.builder()
                        .password(redisPassword)
                        .connectionTimeoutMillis(timeout)
                        .socketTimeoutMillis(timeout)
                        .build());
    }

    @Bean
    public JedisConnectionFactory jedisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, redisPort);
        config.setPassword(redisPassword);
        return new JedisConnectionFactory(config);
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
