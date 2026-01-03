package com.hyp.controller;

import com.hyp.dto.LoginDto;
import com.hyp.dto.VerificationRequestDto;
import com.hyp.entity.Customer;
import com.hyp.exception.BadRequestException;
import com.hyp.exception.EntityNotFoundException;
import com.hyp.observability.ApplicationMetrics;
import com.hyp.observability.MetricTag;
import com.hyp.observability.MetricsEvent;
import com.hyp.response.Response;
import com.hyp.service.AddressService;
import com.hyp.service.CustomerService;
import com.hyp.service.OtpService;
import com.hyp.service.RedisService;
import com.hyp.service.RestaurantService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Slf4j
@RestController
@RequestMapping("/login")
public class LoginController {

    @Autowired
    public CustomerService customerService;

    @Autowired
    public RestaurantService restaurantService;

    @Autowired
    public AddressService addressService;

    @Autowired
    OtpService otpService;

    @Autowired
    private Environment env;

    @Autowired
    HttpServletRequest httpRequest;

    @Autowired
    RedisService redisService;

    @Autowired
    ApplicationMetrics metrics;

    @PostMapping("/otp")
    public ResponseEntity<Response> userLogin(@RequestBody @Valid LoginDto loginDto) throws Exception {
        Customer existingCustomer = customerService.findByMobile(loginDto.getMobile());
        boolean isNewCustomer = existingCustomer == null;

        Customer customer = Optional.ofNullable(existingCustomer).orElseGet(() -> {
            Customer newCustomer = customerService.save(Customer.builder()
                    .name(loginDto.getName())
                    .mobile(loginDto.getMobile())
                    .build());
            metrics.count(MetricsEvent.CUSTOMER, MetricTag.ACTION, "signup", MetricTag.RESULT, "success");

            log.info(
                    "New customer created with mobile ending {}",
                    loginDto.getMobile().substring(loginDto.getMobile().length() - 4));
            return newCustomer;
        });

        metrics.count(MetricsEvent.CUSTOMER, MetricTag.ACTION, "login", MetricTag.RESULT, "attempt");
        if (isInternalUser(customer.getMobile())) {
            otpService.sendOtp(customer.getMobile());
        }
        return ResponseEntity.ok(new Response(null, false, "OTP Sent Successfully"));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<Response> otpVerify(@RequestBody @Valid VerificationRequestDto verificationRequest)
            throws EntityNotFoundException, BadRequestException {
        Customer customer = Optional.ofNullable(customerService.findByMobile(verificationRequest.getMobile()))
                .orElseThrow(() -> {
                    otpService.clearOtp(verificationRequest.getMobile());
                    metrics.count(
                            MetricsEvent.OTP,
                            MetricTag.ACTION,
                            "verify",
                            MetricTag.RESULT,
                            "failed",
                            MetricTag.REASON,
                            "customer_not_found");
                    return new EntityNotFoundException("Login Mobile", verificationRequest.getMobile());
                });

        if (!restaurantService.isExistsById(verificationRequest.getRestaurantId())) {
            otpService.clearOtp(verificationRequest.getMobile());
            metrics.count(
                    MetricsEvent.OTP,
                    MetricTag.ACTION,
                    "verify",
                    MetricTag.RESULT,
                    "failed",
                    MetricTag.REASON,
                    "restaurant_not_found");
            throw new EntityNotFoundException("Restaurant", verificationRequest.getRestaurantId());
        }
        if (isInternalUser(verificationRequest.getMobile())) {
            int storedOtp = otpService.getOtp(verificationRequest.getMobile());
            log.info("storedOTP {} requestedOTP {}", storedOtp, verificationRequest.getOtp());
            if (verificationRequest.getOtp() != storedOtp) {
                metrics.count(
                        MetricsEvent.OTP,
                        MetricTag.ACTION,
                        "verify",
                        MetricTag.RESULT,
                        "failed",
                        MetricTag.REASON,
                        "otp_mismatch");
                throw new BadRequestException("Login", "OTP Verification failed");
            }
            otpService.clearOtp(verificationRequest.getMobile());
        }

        // OTP verified successfully
        metrics.count(MetricsEvent.OTP, MetricTag.ACTION, "verify", MetricTag.RESULT, "success");
        metrics.count(MetricsEvent.CUSTOMER, MetricTag.ACTION, "login", MetricTag.RESULT, "success");

        customer.setVerified(true);

        if (customer.getRestaurants() == null) {
            customer.setRestaurants(new HashSet<>());
        }
        customer.getRestaurants().add(verificationRequest.getRestaurantId());

        customerService.save(customer);
        log.info("OTP verified successfully for customer, restaurantId={}", verificationRequest.getRestaurantId());
        return ResponseEntity.ok(new Response(Collections.singletonList(customer), false, "OTP Verified Successfully"));
    }

    private boolean isInternalUser(String mobile) {
        return !redisService.getInternalUsers().contains(mobile);
    }

    @PostMapping("/resend-otp/{mobile}")
    public ResponseEntity<Response> resendOtp(@PathVariable String mobile) throws Exception {
        if (isInternalUser(mobile)) {
            otpService.clearOtp(mobile);
            otpService.sendOtp(mobile);
        }
        return ResponseEntity.ok(new Response(null, false, "OTP Resent Successfully"));
    }

    @GetMapping("/config")
    public Map<String, Object> getConfig() {
        Map<String, Object> configMap = new HashMap<String, Object>();
        configMap.put("sms.url", env.getProperty("sms.url"));
        configMap.put("sms.key", env.getProperty("sms.key"));
        configMap.put("delivery.pidge.url", env.getProperty("delivery.pidge.url"));
        configMap.put("delivery.pidge.username", env.getProperty("delivery.pidge.username"));
        configMap.put("delivery.pidge.password", env.getProperty("delivery.pidge.password"));
        configMap.put("delivery.pidge.token", env.getProperty("delivery.pidge.token"));
        configMap.put("google.api.key", env.getProperty("google.api.key"));
        configMap.put("pos.petpooja.url", env.getProperty("pos.petpooja.url"));
        configMap.put("pos.petpooja.token", env.getProperty("pos.petpooja.token"));
        configMap.put("pos.petpooja.secret", env.getProperty("pos.petpooja.secret"));
        configMap.put("pos.petpooja.key", env.getProperty("pos.petpooja.key"));
        configMap.put("razorpay.key", env.getProperty("razorpay.key"));
        configMap.put("razorpay.secret", env.getProperty("razorpay.secret"));
        configMap.put("app.domain", env.getProperty("app.domain"));
        configMap.put("spring.data.mongodb.uri", env.getProperty("spring.data.mongodb.uri"));
        String scheme = httpRequest.getScheme();
        String baseUrl = ServletUriComponentsBuilder.fromRequestUri(httpRequest)
                .replacePath(null)
                .build()
                .toUriString();
        configMap.put("domain", httpRequest.getRequestURL().toString() + "======" + scheme + "======" + baseUrl);
        configMap.put("internal.users.numbers", env.getProperty("internal.users.numbers"));

        return configMap;
    }
}
