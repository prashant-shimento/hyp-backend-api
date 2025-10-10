package com.hyp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
public class RedisServiceTest {

    @Mock
    private RedisTemplate<String, String> redisStringTemplate;

    @Mock
    private RedisTemplate<String, Object> redisObjectTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RedisService redisService;

    @BeforeEach
    void setUp() {
        lenient().when(redisStringTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void isNotificationServiceEnabled_ReturnsTrue() {
        when(redisStringTemplate.opsForValue().get("notification:service:enabled"))
                .thenReturn("true");
        boolean result = redisService.isNotificationServiceEnabled();
        assertTrue(result);
        verify(redisStringTemplate.opsForValue(), times(1)).get("notification:service:enabled");
    }

    @Test
    void isNotificationServiceEnabled_ReturnsFalse() {
        when(redisStringTemplate.opsForValue().get("notification:service:enabled"))
                .thenReturn("false");
        boolean result = redisService.isNotificationServiceEnabled();
        assertFalse(result);
        verify(redisStringTemplate.opsForValue(), times(1)).get("notification:service:enabled");
    }

    @Test
    void isNotificationServiceEnabled_ReturnsNull() {
        when(redisStringTemplate.opsForValue().get("notification:service:enabled"))
                .thenReturn(null);
        boolean result = redisService.isNotificationServiceEnabled();
        assertFalse(result);
        verify(redisStringTemplate.opsForValue(), times(1)).get("notification:service:enabled");
    }

    @Test
    void getAlertUsers_Success() {
        String expectedValue = "user1,user2,user3";
        when(redisStringTemplate.opsForValue().get("whatsappAlert")).thenReturn(expectedValue);
        String actualAlertUsers = redisService.getAlertUsers();
        assertNotNull(actualAlertUsers);
    }

    @Test
    void getAlertUsers_ReturnsNull() {
        when(redisStringTemplate.opsForValue().get("whatsappAlert")).thenReturn(null);
        String actualAlertUsers = redisService.getAlertUsers();
        assertNull(actualAlertUsers);
    }

    @Test
    void getInternalUsers_Success() {
        String expectedValue = "user1,user2,user3";
        when(redisStringTemplate.opsForValue().get("internalUsers")).thenReturn(expectedValue);
        String actualInternalUsers = redisService.getInternalUsers();
        assertNotNull(actualInternalUsers);
    }

    @Test
    void getInternalUsers_ReturnsNull() {
        when(redisStringTemplate.opsForValue().get("internalUsers")).thenReturn(null);
        String actualInternalUsers = redisService.getInternalUsers();
        assertNull(actualInternalUsers);
    }

    @Test
    void setRedisData_Success() {
        String key = "Key";
        Object value = "Value";
        long ttl = 2;

        @SuppressWarnings("unchecked")
        ValueOperations<String, Object> valueOperationsMock = mock(ValueOperations.class);
        when(redisObjectTemplate.opsForValue()).thenReturn(valueOperationsMock);
        doNothing().when(valueOperationsMock).set(eq(key), eq(value), eq(Duration.ofSeconds(ttl)));

        redisService.setRedisData(key, value, ttl);

        verify(valueOperationsMock, times(1)).set(eq(key), eq(value), eq(Duration.ofSeconds(ttl)));
        verify(redisObjectTemplate, times(1)).opsForValue();
    }

    @Test
    void setRedisData_Failure() {
        String key = "Key";
        Object value = "Value";
        long ttl = 2;

        @SuppressWarnings("unchecked")
        ValueOperations<String, Object> valueOperationsMock = mock(ValueOperations.class);
        when(redisObjectTemplate.opsForValue()).thenReturn(valueOperationsMock);

        doThrow(new RuntimeException("Redis set operation failed"))
                .when(valueOperationsMock)
                .set(eq(key), eq(value), eq(Duration.ofSeconds(ttl)));

        redisService.setRedisData(key, value, ttl);

        verify(valueOperationsMock, times(1)).set(eq(key), eq(value), eq(Duration.ofSeconds(ttl)));
        verify(redisObjectTemplate, times(1)).opsForValue();
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

        when(redisObjectTemplate.delete(eq(key))).thenReturn(true);

        redisService.removeRedisData(key);

        verify(redisObjectTemplate, times(1)).delete(eq(key));
    }

    @Test
    void removeRedisKey_KeyNotFound() {
        String key = "NonExistentKey";

        when(redisObjectTemplate.delete(eq(key))).thenReturn(false);

        redisService.removeRedisData(key);

        verify(redisObjectTemplate, times(1)).delete(eq(key));
    }
}
