package com.hyp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
public class RedisServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RedisService redisService;

    @BeforeEach
    void setUp() {
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void getAlertUsers_Success() {
        String expectedValue = "user1,user2,user3";
        when(valueOperations.get("whatsappAlert")).thenReturn(expectedValue);
        String actualAlertUsers = redisService.getAlertUsers();
        assertNotNull(actualAlertUsers);
    }

    @Test
    void getAlertUsers_ReturnsNull() {
        when(valueOperations.get("whatsappAlert")).thenReturn(null);
        String actualAlertUsers = redisService.getAlertUsers();
        assertNull(actualAlertUsers);
    }

    @Test
    void getInternalUsers_Success() {
        String expectedValue = "user1,user2,user3";
        when(valueOperations.get("internalUsers")).thenReturn(expectedValue);
        String actualInternalUsers = redisService.getInternalUsers();
        assertNotNull(actualInternalUsers);
    }

    @Test
    void getInternalUsers_ReturnsNull() {
        when(valueOperations.get("internalUsers")).thenReturn(null);
        String actualInternalUsers = redisService.getInternalUsers();
        assertNull(actualInternalUsers);
    }

    @Test
    void setRedisData_Success() throws Exception {
        String key = "Key";
        Object value = "Value";
        long ttl = 2;

        when(objectMapper.writeValueAsString(value)).thenReturn("\"Value\"");

        redisService.setRedisData(key, value, ttl);

        verify(valueOperations, times(1)).set(eq(key), eq("\"Value\""), eq(Duration.ofSeconds(ttl)));
    }

    @Test
    void setRedisData_Failure() throws Exception {
        String key = "Key";
        Object value = "Value";
        long ttl = 2;

        when(objectMapper.writeValueAsString(value)).thenReturn("\"Value\"");
        doThrow(new RuntimeException("Redis set operation failed"))
                .when(valueOperations)
                .set(eq(key), eq("\"Value\""), eq(Duration.ofSeconds(ttl)));

        redisService.setRedisData(key, value, ttl);

        verify(valueOperations, times(1)).set(eq(key), eq("\"Value\""), eq(Duration.ofSeconds(ttl)));
    }

    @Test
    void getRedisData_Success() {
        String key = "Key";
        String expectedValue = "Value";

        when(valueOperations.get(key)).thenReturn(expectedValue);

        Optional<String> result = redisService.getRedisData(key);

        assertTrue(result.isPresent());
        assertEquals(expectedValue, result.get());
        verify(valueOperations, times(1)).get(key);
    }

    @Test
    void getRedisData_KeyNotFound() {
        String key = "Key";
        when(valueOperations.get(key)).thenReturn(null);
        Optional<String> result = redisService.getRedisData(key);
        assertFalse(result.isPresent());
        verify(valueOperations, times(1)).get(key);
    }

    @Test
    void removeRedisKey_Success() {
        String key = "Key";

        when(stringRedisTemplate.delete(eq(key))).thenReturn(true);

        redisService.removeRedisData(key);

        verify(stringRedisTemplate, times(1)).delete(eq(key));
    }

    @Test
    void removeRedisKey_KeyNotFound() {
        String key = "NonExistentKey";

        when(stringRedisTemplate.delete(eq(key))).thenReturn(false);

        redisService.removeRedisData(key);

        verify(stringRedisTemplate, times(1)).delete(eq(key));
    }
}
