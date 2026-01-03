package com.hyp.service;

import com.hyp.exception.EntityNotFoundException;
import com.hyp.observability.ApplicationMetrics;
import com.hyp.observability.MetricTag;
import com.hyp.observability.MetricsEvent;
import java.security.SecureRandom;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class OtpService {

    @Value("${sms.url}")
    private String smsBaseUrl;

    @Value("${sms.key}")
    private String smsKey;

    private static final int EXPIRE_MINS = 1;

    private final RedisService redisService;
    private final WebClient webClient;
    private final ApplicationMetrics metrics;

    @Autowired
    public OtpService(RedisService redisService, ApplicationMetrics metrics) {
        this.redisService = redisService;
        this.webClient = WebClient.builder().build();
        this.metrics = metrics;
    }

    public void sendOtp(String mobileNum) throws Exception {
        int otp = generateOtp(mobileNum);

        String smsUrl = smsBaseUrl
                .replace("{key}", smsKey)
                .replace("{mobile}", mobileNum)
                .replace("{otp}", String.valueOf(otp));

        try {
            Mono<String> responseMono = webClient.get().uri(smsUrl).retrieve().bodyToMono(String.class);

            responseMono
                    .map(response -> new JSONObject(response).optString("Status"))
                    .map("Success"::equalsIgnoreCase)
                    .block();

            metrics.count(MetricsEvent.OTP, MetricTag.ACTION, "send", MetricTag.RESULT, "success");
            log.info("OTP sent successfully to mobile ending with {}", mobileNum.substring(mobileNum.length() - 4));
        } catch (Exception e) {
            metrics.count(MetricsEvent.OTP, MetricTag.ACTION, "send", MetricTag.RESULT, "failed");
            log.error("Error while sending OTP SMS {}", e.getMessage());
            throw new Exception(e.getMessage());
        }
    }

    public int generateOtp(String key) {
        int otp = new SecureRandom().nextInt(900000) + 100000;
        redisService.setRedisData(
                "otp:" + key, otp, Duration.ofMinutes(EXPIRE_MINS).toSeconds());
        return otp;
    }

    public int getOtp(String key) throws EntityNotFoundException {
        return redisService
                .getRedisData("otp:" + key)
                .map(Integer::parseInt)
                .orElseThrow(() -> new EntityNotFoundException("OTP", key));
    }

    public void clearOtp(String key) {
        redisService.removeRedisData("otp:" + key);
    }
}
