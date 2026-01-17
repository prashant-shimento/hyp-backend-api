package com.hyp.controller;

import com.hyp.dto.ReferralTokenDto;
import com.hyp.entity.ReferralToken;
import com.hyp.exception.BadRequestException;
import com.hyp.request.CreateReferralTokenRequest;
import com.hyp.response.Response;
import com.hyp.service.ReferralTokenService;
import com.hyp.translation.ReferralTokenTranslation;
import jakarta.validation.Valid;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/referral/token")
public class ReferralTokenController extends BaseListController<ReferralTokenDto, ReferralToken, String> {

    @Autowired
    private ReferralTokenTranslation referralTokenTranslation;

    @Autowired
    private ReferralTokenService referralTokenService;

    /**
     * Creates a referral token when user clicks a referral link.
     * Called by Discovery Platform frontend before redirecting to restaurant.
     * <p>
     * POST /api/v2/referral/token
     * {
     *   "referralCode": "FRIEND123",
     *   "restaurantId": "rest_abc",
     *   "source": "instagram"  // optional
     * }
     */
    @PostMapping
    public ResponseEntity<Response> createToken(@Valid @RequestBody CreateReferralTokenRequest request)
            throws BadRequestException {
        log.info(
                "Creating referral token: code={}, restaurant={}, source={}",
                request.getReferralCode(),
                request.getRestaurantId(),
                request.getSource());

        ReferralToken referralToken = referralTokenService.createToken(
                request.getReferralCode(), request.getRestaurantId(), request.getSource());

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("referralToken", referralToken.getToken());
        responseData.put("expiresAt", referralToken.getExpiresAt().toString());

        return ResponseEntity.ok(Response.builder()
                .data(Collections.singletonList(responseData))
                .error(false)
                .message("Referral token created successfully")
                .build());
    }
}
