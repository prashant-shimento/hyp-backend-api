package com.hyp.service;

import static com.hyp.util.CommonUtils.generateSecureToken;

import com.hyp.entity.Customer;
import com.hyp.entity.CustomerReferral;
import com.hyp.entity.ReferralCode;
import com.hyp.entity.ReferralToken;
import com.hyp.exception.BadRequestException;
import com.hyp.repository.CustomerReferralRepository;
import com.hyp.repository.ReferralCodeRepository;
import com.hyp.repository.ReferralTokenRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReferralTokenService extends BaseServiceImpl<ReferralToken, String> {

    private final ReferralTokenRepository referralTokenRepository;
    private final ReferralCodeRepository referralCodeRepository;
    private final CustomerReferralRepository customerReferralRepository;

    /**
     * Creates a referral token when user clicks a referral link.
     */
    public ReferralToken createToken(String referralCode, String restaurantId, String source)
            throws BadRequestException {
        log.info("Creating referral token for code={}, restaurant={}", referralCode, restaurantId);

        ReferralCode code = referralCodeRepository
                .findByCodeAndActiveTrueAndIsDeletedFalse(referralCode)
                .orElseThrow(() -> new BadRequestException("Referral", "Invalid or inactive referral code"));

        String token = generateSecureToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(code.getAttributionWindowDays());

        ReferralToken referralToken = ReferralToken.builder()
                .token(token)
                .referralCodeId(code.getId())
                .referralCode(code.getCode())
                .restaurantId(restaurantId)
                .source(source)
                .expiresAt(expiresAt)
                .createdAt(LocalDateTime.now())
                .build();

        referralToken = referralTokenRepository.save(referralToken);
        log.info("Referral token created: id={}, expiresAt={}", referralToken.getId(), expiresAt);

        return referralToken;
    }

    /**
     * Validates a token and returns the referral token if valid.
     */
    public ReferralToken validateToken(String token, String restaurantId) throws BadRequestException {
        ReferralToken referralToken = referralTokenRepository
                .findByToken(token)
                .orElseThrow(() -> new BadRequestException("Referral", "Invalid token"));

        if (LocalDateTime.now().isAfter(referralToken.getExpiresAt())) {
            throw new BadRequestException("Referral", "Token has expired");
        }

        if (!referralToken.getRestaurantId().equals(restaurantId)) {
            throw new BadRequestException("Referral", "Token not valid for this restaurant");
        }

        return referralToken;
    }

    /**
     * Finds a valid token by token value (returns null if invalid/expired).
     */
    public ReferralToken findValidToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }

        return referralTokenRepository
                .findByToken(token)
                .filter(referralToken -> LocalDateTime.now().isBefore(referralToken.getExpiresAt()))
                .orElse(null);
    }

    /**
     * Links a customer to a referral for a specific restaurant.
     * Only creates referral if customer is NEW to this restaurant.
     * - New customer + new restaurant = valid
     * - Existing customer + new restaurant = valid
     * - Existing customer + already mapped restaurant = NOT valid
     */
    public void linkCustomerReferral(Customer customer, String restaurantId, ReferralToken referralToken) {
        // Check if customer already uses this restaurant (not a true referral)
        String customerId = customer.getId();
        if (customer.getRestaurants() != null && customer.getRestaurants().contains(restaurantId)) {
            log.info("Customer {} already mapped to restaurant {}, not a valid referral", customerId, restaurantId);
            return;
        }

        CustomerReferral referral = CustomerReferral.builder()
                .customerId(customerId)
                .restaurantId(restaurantId)
                .referralCode(referralToken.getReferralCode())
                .referralCodeId(referralToken.getReferralCodeId())
                .referralTokenId(referralToken.getId())
                .build();

        try {
            customerReferralRepository.save(referral);
            referralToken.setCustomerId(customerId);
            referralTokenRepository.save(referralToken);
            log.info(
                    "Customer {} linked to referral {} for restaurant {}",
                    customerId,
                    referralToken.getReferralCode(),
                    restaurantId);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            log.info("Customer {} already has referral for restaurant {}, ignoring", customerId, restaurantId);
        }
    }

    /**
     * Validates a token for order attribution WITHOUT consuming it.
     * Used at order creation - token is consumed only on successful payment.
     */
    public ReferralToken validateTokenForOrder(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }

        ReferralToken referralToken = findValidToken(token);
        if (referralToken == null) {
            return null;
        }

        // Check if token already used for a paid order
        if (referralToken.getUsedOrder() != null) {
            log.debug("Token {} already used for order {}", referralToken.getId(), referralToken.getUsedOrder());
            return null;
        }

        return referralToken;
    }

    /**
     * Consumes a token on successful payment (single-use enforcement).
     * Called when order status changes to PAID.
     * Uses token ID (stored on order) for lookup.
     */
    public boolean consumeTokenForPaidOrder(String tokenId, String orderId) {
        if (tokenId == null || tokenId.isBlank()) {
            return false;
        }

        Optional<ReferralToken> tokenOpt = referralTokenRepository.findById(tokenId);
        if (tokenOpt.isEmpty()) {
            log.debug("Token not found for id={}", tokenId);
            return false;
        }

        ReferralToken referralToken = tokenOpt.get();

        // Check if token already used by another order
        if (referralToken.getUsedOrder() != null
                && !referralToken.getUsedOrder().equals(orderId)) {
            log.info(
                    "Token {} already consumed by order {}, current order {}",
                    tokenId,
                    referralToken.getUsedOrder(),
                    orderId);
            return false;
        }

        // Mark token as used
        referralToken.setUsedOrder(orderId);
        referralTokenRepository.save(referralToken);
        log.info("Token {} consumed for paid order {}", tokenId, orderId);
        return true;
    }
}
