package com.hyp.service;

import static com.hyp.enums.FeeComponent.PLATFORM_FEE;

import com.hyp.constants.Constants;
import com.hyp.entity.*;
import com.hyp.enums.*;
import com.hyp.enums.OrderType;
import com.hyp.model.FeeRule;
import com.hyp.repository.SettlementRepository;
import com.hyp.request.SettlementRequest;
import com.hyp.util.CommonUtils;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SettlementService extends BaseServiceImpl<Settlement, String> {

    @Autowired
    SettlementRepository settlementRepository;

    @Autowired
    DeliveryService deliveryService;

    @Autowired
    RestaurantService restaurantService;

    @Autowired
    FeeService feeService;

    @Autowired
    OrderService orderService;

    @Autowired
    PartnerService partnerService;

    public Settlement findByOrderId(String orderId) {
        return settlementRepository.findByOrderId(orderId);
    }

    private final ExecutorService settlementExecutor = Executors.newFixedThreadPool(5);

    public void processSettlement(SettlementRequest request) {
        log.info(
                "Async settlement job started for restaurant {} between {} and {}",
                request.getRestaurantId(),
                request.getStartDate(),
                request.getEndDate());

        CompletableFuture.runAsync(
                () -> {
                    try {

                        Restaurant restaurant = restaurantService.findById(request.getRestaurantId());
                        if (restaurant == null) {
                            log.error("Restaurant not found: {}", request.getRestaurantId());
                            return;
                        }

                        Fee feeConfig = feeService.findByRestaurantId(request.getRestaurantId());
                        if (feeConfig == null || feeConfig.isDeleted() || !feeConfig.isActive()) {
                            log.error(
                                    "Fee configuration missing or inactive for restaurant {}",
                                    request.getRestaurantId());
                            return;
                        }

                        Partner partner =
                                partnerService.findPartnersByRestaurantId(restaurant.getId(), PartnerType.RESTAURANT);

                        List<Order> orders = orderService.findByRestaurantIdAndCreatedAt(
                                request.getRestaurantId(),
                                OrderStatusType.DELIVERED,
                                request.getStartDate(),
                                request.getEndDate());

                        if (orders.isEmpty()) {
                            log.info(
                                    "No delivered orders found for restaurant {} in the date range",
                                    request.getRestaurantId());
                            return;
                        }
                        // Process orders sequentially or parallel
                        orders.forEach(order -> {
                            try {
                                computeSettlement(order, restaurant, feeConfig, partner);
                            } catch (Exception e) {
                                log.error(
                                        "Failed to process settlement for order {}: {}", order.getId(), e.getMessage());
                            }
                        });

                        log.info("Settlement processing completed for restaurant {}", request.getRestaurantId());

                    } catch (Exception ex) {
                        log.error(
                                "Error in async settlement processing for restaurant {}: {}",
                                request.getRestaurantId(),
                                ex.getMessage(),
                                ex);
                    }
                },
                settlementExecutor);
    }

    public Settlement processSettlement(Order order) {
        String orderId = order.getId();
        String restaurantId = order.getRestaurantId();
        Partner partner = null;
        log.info("Starting settlement computation for orderId={}, restaurantId={}", orderId, restaurantId);

        Restaurant restaurant = restaurantService.findById(restaurantId);
        if (order.getPartnerId() == null) {
            if (restaurant.getRestaurantPartner() != null) {
                partner = new Partner();
                partner.setId(restaurant.getRestaurantPartner());
            } else {
                partner = partnerService.findPartnersByRestaurantId(restaurantId, PartnerType.RESTAURANT);
            }
        }

        Fee feeConfig = feeService.findByRestaurantId(restaurantId);

        if (feeConfig == null) {
            log.error("Fee configuration missing for restaurant {}", restaurantId);
            throw new RuntimeException("Fee configuration not available for restaurant " + restaurantId);
        }
        return computeSettlement(order, restaurant, feeConfig, partner);
    }

    private Settlement computeSettlement(Order order, Restaurant restaurant, Fee feeConfig, Partner partner) {
        String orderId = order.getId();
        String restaurantId = order.getRestaurantId();
        String partnerId = order.getPartnerId();

        // Determine partner
        if (partnerId == null && partner != null) {
            partnerId = partner.getId();
        }

        // Fetch existing settlement
        Settlement settlement = this.findByOrderId(orderId);
        if (settlement == null) {
            settlement = new Settlement();
            settlement.setOrderId(orderId);
            settlement.setRestaurantId(restaurantId);
            settlement.setPartnerId(partnerId);
            log.info("Creating new Settlement for orderId {}", orderId);
        }

        // Compute item total
        double itemTotal = order.getItemTotalAmount();
        if (itemTotal == 0) {
            itemTotal = order.getOrderItems().stream()
                    .mapToDouble(item -> {
                        double addonTotal =
                                Optional.ofNullable(item.getOrderAddonItems()).orElse(Collections.emptyList()).stream()
                                        .mapToDouble(a -> a.getPrice() * a.getQuantity())
                                        .sum();
                        return (item.getPrice() * item.getQuantity()) + addonTotal;
                    })
                    .sum();
        }

        double discount = order.getDiscountAmount();
        double tax = order.getTaxAmount();
        double bill = itemTotal - discount;
        double netBill = bill + tax;

        // Compute delivery charge
        double deliveryCharge = 0;
        if (OrderType.H == OrderType.fromCode(order.getOrderType())) {
            Delivery delivery = deliveryService.findByOrderId(orderId);
            if (delivery != null) {
                deliveryCharge = delivery.getFulfillment().getDeliveryCharge();
            }
        }

        // Compute fees
        Map<FeeComponent, Double> appliedFees = new EnumMap<>(FeeComponent.class);
        if (feeConfig.getFeeRules() != null) {
            for (FeeRule feeRule : feeConfig.getFeeRules()) {
                if (!feeRule.isActive()) continue;
                double feeAmount = getFeeAmount(feeRule, bill, netBill);
                appliedFees.put(feeRule.getFee(), feeAmount);
            }
        }

        double totalFees =
                appliedFees.values().stream().mapToDouble(Double::doubleValue).sum();

        // Delivery share
        double platformDeliveryShare = deliveryCharge
                * Optional.ofNullable(restaurant.getPlatformDeliveryShare()).orElse(Constants.PLATFORM_DELIVERY_SHARE)
                / 100.0;
        double merchantDeliveryShare = getMerchantDeliveryShare(restaurant, netBill, platformDeliveryShare);

        double totalSettlement = netBill - totalFees + merchantDeliveryShare;

        // Populate settlement
        settlement.setItemTotal(CommonUtils.roundToTwoDecimal(itemTotal));
        settlement.setDiscount(discount);
        settlement.setTax(tax);
        settlement.setBill(bill);
        settlement.setNetBill(netBill);
        settlement.setPlatformFee(CommonUtils.roundToTwoDecimal(appliedFees.getOrDefault(PLATFORM_FEE, 0.0)));
        settlement.setPosFee(CommonUtils.roundToTwoDecimal(appliedFees.getOrDefault(FeeComponent.POS_FEE, 0.0)));
        settlement.setPaymentGatewayFee(
                CommonUtils.roundToTwoDecimal(appliedFees.getOrDefault(FeeComponent.PAYMENT_GATEWAY_FEE, 0.0)));
        settlement.setTotalFees(CommonUtils.roundToTwoDecimal(totalFees));
        settlement.setPlatformDeliveryShare(platformDeliveryShare);
        settlement.setMerchantDeliveryShare(merchantDeliveryShare);
        settlement.setTotalSettlement(CommonUtils.roundToTwoDecimal(totalSettlement));
        settlement.setSettlementStatus(SettlementStatus.PENDING.name());
        settlement.setRestaurantId(restaurantId);
        settlement.setFeesApplied(feeConfig.getFeeRules());
        settlement.setDeliveryCharge(deliveryCharge);
        settlement.setOrderAt(order.getCreatedAt());
        save(settlement);
        log.info("Settlement created for order {} restaurant {}: total {}", orderId, restaurantId, totalSettlement);
        return settlement;
    }

    private static double getFeeAmount(FeeRule feeRule, double bill, double netBill) {
        double baseAmount = PLATFORM_FEE.equals(feeRule.getFee()) ? bill : netBill;

        return switch (feeRule.getType()) {
            case PERCENTAGE -> baseAmount * feeRule.getValue() / 100.0;
            case FIXED -> feeRule.getValue();
            case THRESHOLD -> baseAmount > feeRule.getThresholdValue() ? feeRule.getMaxValue() : feeRule.getMinValue();
            default -> 0;
        };
    }

    private static double getMerchantDeliveryShare(
            Restaurant restaurant, double netBill, double platformDeliveryShare) {
        Double restaurantDeliveryShare = restaurant.getRestaurantDeliveryShare();
        int deliveryOfferThreshold = restaurant.getDeliveryOffer();

        double merchantDeliveryShare;

        if (deliveryOfferThreshold != 0 && netBill >= deliveryOfferThreshold) {
            merchantDeliveryShare = platformDeliveryShare;
        } else {
            merchantDeliveryShare =
                    restaurantDeliveryShare != null ? platformDeliveryShare * restaurantDeliveryShare / 100.0 : 0;
        }
        return merchantDeliveryShare;
    }
}
