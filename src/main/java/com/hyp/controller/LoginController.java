package com.hyp.controller;

import com.hyp.dto.LoginDto;
import com.hyp.dto.VerificationRequestDto;
import com.hyp.entity.Customer;
import com.hyp.entity.ReferralToken;
import com.hyp.exception.BadRequestException;
import com.hyp.exception.EntityNotFoundException;
import com.hyp.observability.ApplicationMetrics;
import com.hyp.observability.MetricTag;
import com.hyp.observability.MetricsEvent;
import com.hyp.response.Response;
import com.hyp.security.jwt.JwtTokenService;
import com.hyp.security.principal.UserPrincipal;
import com.hyp.service.CustomerService;
import com.hyp.service.OtpService;
import com.hyp.service.RedisService;
import com.hyp.service.ReferralTokenService;
import com.hyp.service.RestaurantService;
import jakarta.validation.Valid;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping(path = {"/api/v2/login", "/api/v3/login"})
@RequiredArgsConstructor
public class LoginController {

    private final CustomerService customerService;
    private final RestaurantService restaurantService;
    private final OtpService otpService;
    private final RedisService redisService;
    private final JwtTokenService jwtTokenService;
    private final ApplicationMetrics metrics;
    private final ReferralTokenService referralTokenService;

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
        otpService.sendOtp(customer.getMobile());
        return ResponseEntity.ok(new Response(null, false, "OTP Sent Successfully"));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<Response> otpVerify(@RequestBody @Valid VerificationRequestDto verificationRequest)
            throws EntityNotFoundException, BadRequestException {
        Customer customer = Optional.ofNullable(customerService.findByMobile(verificationRequest.getMobile()))
                .orElseThrow(() -> {
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
        // Verify OTP (clears on success, metrics handled in service)
        otpService.verifyOtp(verificationRequest.getMobile(), String.valueOf(verificationRequest.getOtp()));
        metrics.count(MetricsEvent.CUSTOMER, MetricTag.ACTION, "login", MetricTag.RESULT, "success");
        customer.setVerified(true);

        // First-touch attribution (analytics only)
        // Does NOT mark token as used or count referral
        if (verificationRequest.getReferralToken() != null
                && !verificationRequest.getReferralToken().isBlank()) {
            try {
                ReferralToken referralToken = referralTokenService.validateToken(
                        verificationRequest.getReferralToken(), verificationRequest.getRestaurantId());
                referralTokenService.linkCustomerReferral(
                        customer, verificationRequest.getRestaurantId(), referralToken);
                log.info(
                        "Referral linked for customer={}, restaurant={}, referralCode={}",
                        customer.getId(),
                        verificationRequest.getRestaurantId(),
                        referralToken.getReferralCode());
            } catch (Exception e) {
                log.warn("Failed to link referral for customer {}: {}", customer.getId(), e.getMessage());
            }
        }

        Set<String> restaurants = Optional.ofNullable(customer.getRestaurants()).orElseGet(() -> {
            Set<String> s = new HashSet<>();
            customer.setRestaurants(s);
            return s;
        });

        boolean added = restaurants.add(verificationRequest.getRestaurantId());

        if (added) {
            customerService.save(customer);
        }
        log.info("OTP verified successfully for customer, restaurantId={}", verificationRequest.getRestaurantId());

        // Generate JWT tokens for authenticated customer
        UserPrincipal principal = UserPrincipal.builder()
                .userId(customer.getId())
                .userType("CUSTOMER")
                .role("CUSTOMER")
                .mobile(customer.getMobile())
                .name(customer.getName())
                .restaurantId(verificationRequest.getRestaurantId())
                .build();

        Map<String, Object> tokens = jwtTokenService.createTokenPair(principal, null, null);

        // Build response with customer data and tokens
        Map<String, Object> responseData = new LinkedHashMap<>();
        responseData.put("customer", customer);
        responseData.put("accessToken", tokens.get("accessToken"));
        responseData.put("refreshToken", tokens.get("refreshToken"));
        responseData.put("tokenType", tokens.get("tokenType"));
        responseData.put("expiresIn", tokens.get("expiresIn"));

        return ResponseEntity.ok(
                new Response(Collections.singletonList(responseData), false, "OTP Verified Successfully"));
    }

    @PostMapping("/resend-otp/{mobile}")
    public ResponseEntity<Response> resendOtp(@PathVariable String mobile) throws BadRequestException {
        otpService.resendOtp(mobile);
        return ResponseEntity.ok(new Response(null, false, "OTP Resent Successfully"));
    }
}
