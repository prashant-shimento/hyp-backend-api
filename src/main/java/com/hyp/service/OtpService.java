package com.hyp.service;

import static com.hyp.util.CommonUtils.maskMobile;

import com.hyp.exception.BadRequestException;
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

@Slf4j
@Component
public class OtpService {

    @Value("${sms.url}")
    private String smsBaseUrl;

    @Value("${sms.key}")
    private String smsKey;

    private static final Duration OTP_TTL = Duration.ofSeconds(100);
    private static final String OTP_KEY_PREFIX = "otp:";

    private final RedisService redisService;
    private final WebClient webClient;
    private final ApplicationMetrics metrics;

    @Autowired
    public OtpService(RedisService redisService, ApplicationMetrics metrics) {
        this.redisService = redisService;
        this.webClient = WebClient.builder().build();
        this.metrics = metrics;
    }

    /**
     * Send OTP to mobile number.
     * Fails if OTP already exists (not expired).
     */
    public void sendOtp(String mobile) throws BadRequestException {
        int otp = generateOtp();
        boolean created = redisService.setIfAbsent(otpKey(mobile), String.valueOf(otp), OTP_TTL);
        if (!created) {
            log.info("OTP already exists for {}", maskMobile(mobile));
            throw new BadRequestException("OTP", "OTP already sent. Please wait before requesting again.");
        }
        sendSms(mobile, otp);
    }

    /**
     * Resend OTP - clears existing and sends new.
     */
    public void resendOtp(String mobile) throws BadRequestException {
        sendOtp(mobile);
    }

    /**
     * Verify OTP and clear on success.
     */
    public void verifyOtp(String mobile, String otp) throws BadRequestException, EntityNotFoundException {
        String storedOtp =
                redisService.getRedisData(otpKey(mobile)).orElseThrow(() -> new EntityNotFoundException("OTP", mobile));

        if (!storedOtp.equals(otp)) {
            metrics.count(MetricsEvent.OTP, MetricTag.ACTION, "verify", MetricTag.RESULT, "failed");
            throw new BadRequestException("OTP", "Invalid OTP");
        }

        redisService.removeRedisData(otpKey(mobile));
        metrics.count(MetricsEvent.OTP, MetricTag.ACTION, "verify", MetricTag.RESULT, "success");
        log.info("OTP verified for {}", maskMobile(mobile));
    }

    private void sendSms(String mobile, int otp) {
        String smsUrl =
                smsBaseUrl.replace("{key}", smsKey).replace("{mobile}", mobile).replace("{otp}", String.valueOf(otp));

        webClient
                .get()
                .uri(smsUrl)
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> new JSONObject(response).optString("Status"))
                .subscribe(
                        status -> {
                            if ("Success".equalsIgnoreCase(status)) {
                                metrics.count(MetricsEvent.OTP, MetricTag.ACTION, "send", MetricTag.RESULT, "success");
                                log.info("OTP sent to {}", maskMobile(mobile));
                            } else {
                                handleSmsFailed(mobile, "SMS non-success response");
                            }
                        },
                        error -> handleSmsFailed(mobile, error.getMessage()));
    }

    private void handleSmsFailed(String mobile, String reason) {
        redisService.removeRedisData(otpKey(mobile));
        metrics.count(MetricsEvent.OTP, MetricTag.ACTION, "send", MetricTag.RESULT, "failed");
        log.error("OTP send failed for {} reason={}", maskMobile(mobile), reason);
    }

    private int generateOtp() {
        return new SecureRandom().nextInt(900_000) + 100_000;
    }

    private String otpKey(String mobile) {
        return OTP_KEY_PREFIX + mobile;
    }
}
