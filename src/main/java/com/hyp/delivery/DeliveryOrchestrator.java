package com.hyp.delivery;

import com.hyp.entity.Address;
import com.hyp.entity.Customer;
import com.hyp.entity.Delivery;
import com.hyp.entity.Order;
import com.hyp.entity.Restaurant;
import com.hyp.enums.DeliveryFulfillStatusType;
import com.hyp.enums.DeliveryLifecycleEvent;
import com.hyp.enums.DeliveryOrderStatusType;
import com.hyp.enums.DeliveryPartner;
import com.hyp.exception.DeliveryException;
import com.hyp.model.DeliveryOrderStatus;
import com.hyp.util.CommonUtils;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Routes delivery orders to the optimal partner based on restaurant config and real-time
 * partner health. Primary partner is always tried first; secondary is the automatic fallback
 * when primary is blocked or fails on API call.
 *
 * Routing logic:
 *   1. Load restaurant config (primaryPartner, secondaryPartner, assignmentSlaMinutes)
 *   2. Check DeliveryPartnerEvaluator — is primary blocked?
 *   3. If blocked → use secondary; if both blocked → throw DeliveryException
 *   4. Call provider.createOrder(); on API failure → record event, try secondary
 *   5. Return unsaved Delivery entity (caller is responsible for saving)
 */
@Slf4j
@Service
public class DeliveryOrchestrator {

    private static final int DEFAULT_ASSIGNMENT_SLA_MINUTES = 8;
    private static final int DEFAULT_PICKUP_SLA_MINUTES = 25;
    private static final int DEFAULT_DELIVERY_SLA_MINUTES = 45;

    private final Map<DeliveryPartner, DeliveryProvider> registry;
    private final DeliveryPartnerEvaluator evaluator;
    private final DeliveryLifecycleService lifecycleService;

    public DeliveryOrchestrator(
            List<DeliveryProvider> providers,
            DeliveryPartnerEvaluator evaluator,
            DeliveryLifecycleService lifecycleService) {
        this.registry = providers.stream().collect(Collectors.toMap(DeliveryProvider::getType, Function.identity()));
        this.evaluator = evaluator;
        this.lifecycleService = lifecycleService;
        log.info("DeliveryOrchestrator registered providers: {}", registry.keySet());
    }

    // ─── Primary API ─────────────────────────────────────────────────────────

    /**
     * Select a partner and create the delivery order.
     * Returns an unsaved Delivery — caller must persist it.
     */
    public Delivery createDelivery(Order order, Restaurant restaurant, Address address, Customer customer)
            throws DeliveryException {
        DeliveryPartner selected = selectPartner(restaurant, order.getId());

        try {
            return createWithProvider(selected, order, restaurant, address, customer);
        } catch (DeliveryException e) {
            log.warn("Partner {} API failure for orderId={}: {}", selected, order.getId(), e.getMessage());
            evaluator.recordOutcome(selected, restaurant.getId(), DeliveryLifecycleEvent.API_FAILURE, false);
            lifecycleService.record(
                    order.getId(), null, restaurant.getId(), selected, DeliveryLifecycleEvent.API_FAILURE, false);

            DeliveryPartner fallback = getFallback(restaurant, selected);
            if (fallback == null) {
                throw new DeliveryException(
                        "createDelivery", "All delivery partners unavailable for orderId=" + order.getId());
            }
            log.info("Falling back to {} for orderId={}", fallback, order.getId());
            lifecycleService.record(
                    order.getId(), null, restaurant.getId(), selected, DeliveryLifecycleEvent.PROVIDER_SWITCHED, false);
            return createWithProvider(fallback, order, restaurant, address, customer);
        }
    }

    /**
     * Cancel the active delivery and create a new one with the fallback partner.
     * Called from DeliveryService.switchDeliveryProvider (assignment timeout or manual ops).
     */
    public Delivery switchDelivery(
            Delivery active, Order order, Restaurant restaurant, Address address, Customer customer)
            throws DeliveryException {
        DeliveryProvider currentProvider = getProvider(active.getProvider());

        try {
            currentProvider.cancelOrder(active);
        } catch (DeliveryException e) {
            log.warn(
                    "Cancel failed for delivery={} provider={}: {}",
                    active.getId(),
                    active.getProvider(),
                    e.getMessage());
        }

        evaluator.recordOutcome(
                active.getProvider(), restaurant.getId(), DeliveryLifecycleEvent.ASSIGNMENT_TIMEOUT, false);
        lifecycleService.record(
                order.getId(),
                active.getId(),
                restaurant.getId(),
                active.getProvider(),
                DeliveryLifecycleEvent.ASSIGNMENT_TIMEOUT,
                false);
        lifecycleService.record(
                order.getId(),
                active.getId(),
                restaurant.getId(),
                active.getProvider(),
                DeliveryLifecycleEvent.PROVIDER_SWITCHED,
                false);

        DeliveryPartner next = getFallback(restaurant, active.getProvider());
        if (next == null) {
            throw new DeliveryException("switchDelivery", "No available fallback partner for orderId=" + order.getId());
        }

        log.info("Switching from {} to {} for orderId={}", active.getProvider(), next, order.getId());
        return createWithProvider(next, order, restaurant, address, customer);
    }

    // ─── Assignment SLA ───────────────────────────────────────────────────────

    public int getAssignmentSlaMinutes(Restaurant restaurant) {
        Restaurant.DeliveryConfig config = restaurant.getDeliveryConfig();
        return config != null && config.getAssignSla() > 0 ? config.getAssignSla() : DEFAULT_ASSIGNMENT_SLA_MINUTES;
    }

    public int getPickupSlaMinutes(Restaurant restaurant) {
        Restaurant.DeliveryConfig config = restaurant.getDeliveryConfig();
        return config != null && config.getPickupSla() > 0 ? config.getPickupSla() : DEFAULT_PICKUP_SLA_MINUTES;
    }

    public int getDeliverySlaMinutes(Restaurant restaurant) {
        Restaurant.DeliveryConfig config = restaurant.getDeliveryConfig();
        return config != null && config.getDeliverySla() > 0 ? config.getDeliverySla() : DEFAULT_DELIVERY_SLA_MINUTES;
    }

    // ─── Provider access (used by DeliveryService for cancel, status, etc.) ──

    public DeliveryProvider getProvider(DeliveryPartner type) {
        DeliveryProvider provider = registry.get(type);
        if (provider == null) {
            throw new IllegalArgumentException("No DeliveryProvider registered for: " + type);
        }
        return provider;
    }

    // ─── Internal ────────────────────────────────────────────────────────────

    private DeliveryPartner selectPartner(Restaurant restaurant, String orderId) throws DeliveryException {
        Restaurant.DeliveryConfig config = restaurant.getDeliveryConfig();
        if (config == null || config.getPrimaryPartner() == null) {
            log.info("No delivery config for restaurant={}, defaulting to PIDGE", restaurant.getId());
            return DeliveryPartner.PIDGE;
        }

        DeliveryPartner primary = config.getPrimaryPartner();
        if (!evaluator.isBlocked(primary, restaurant.getId())) {
            return primary;
        }

        log.warn("Primary partner {} is blocked for restaurant={}, trying secondary", primary, restaurant.getId());

        DeliveryPartner secondary = config.getSecondaryPartner();
        if (secondary != null && !evaluator.isBlocked(secondary, restaurant.getId())) {
            return secondary;
        }

        throw new DeliveryException(
                "selectPartner",
                "All delivery partners blocked for restaurant=" + restaurant.getId() + " orderId=" + orderId);
    }

    private DeliveryPartner getFallback(Restaurant restaurant, DeliveryPartner failed) {
        Restaurant.DeliveryConfig config = restaurant.getDeliveryConfig();
        if (config == null) return null;

        DeliveryPartner secondary = config.getSecondaryPartner();
        if (secondary != null && secondary != failed && !evaluator.isBlocked(secondary, restaurant.getId())) {
            return secondary;
        }

        // Also try primary if secondary was the one that failed
        DeliveryPartner primary = config.getPrimaryPartner();
        if (primary != null && primary != failed && !evaluator.isBlocked(primary, restaurant.getId())) {
            return primary;
        }

        return null;
    }

    private Delivery createWithProvider(
            DeliveryPartner partnerType, Order order, Restaurant restaurant, Address address, Customer customer)
            throws DeliveryException {
        long start = System.currentTimeMillis();
        DeliveryProvider provider = getProvider(partnerType);

        DeliveryProvider.CreateOrderResult result = provider.createOrder(restaurant, address, customer, order);
        long durationMs = System.currentTimeMillis() - start;

        lifecycleService.record(
                order.getId(),
                null,
                restaurant.getId(),
                partnerType,
                DeliveryLifecycleEvent.ORDER_CREATED,
                true,
                durationMs,
                null);

        log.info(
                "Delivery order created provider={} orderId={} providerOrderId={} durationMs={}",
                partnerType,
                order.getId(),
                result.orderId(),
                durationMs);

        return buildDelivery(order, partnerType, result.orderId(), result.fee(), restaurant, address, customer);
    }

    private Delivery buildDelivery(
            Order order,
            DeliveryPartner provider,
            String providerOrderId,
            Double fee,
            Restaurant restaurant,
            Address address,
            Customer customer) {
        Delivery delivery = new Delivery();
        delivery.setId(CommonUtils.genId());
        delivery.setOrderId(order.getId());
        delivery.setReferenceId(order.getId());
        delivery.setDeliveryOrderId(providerOrderId);
        delivery.setProvider(provider);
        delivery.setStatus(DeliveryOrderStatusType.PENDING);
        delivery.setSwitchable(true);

        if (order.getDeliveryDetails() != null) {
            delivery.setService(order.getDeliveryDetails().getService());
        }
        // Fee is returned by Adloggs on order creation — store it in fulfillment.
        // Pidge fee comes from the quote estimate, not the create response.
        if (fee != null) {
            DeliveryOrderStatus.DeliveryFulfillment fulfillment = new DeliveryOrderStatus.DeliveryFulfillment();
            fulfillment.setStatus(DeliveryFulfillStatusType.CREATED);
            fulfillment.setDeliveryCharge(fee);
            delivery.setFulfillment(fulfillment);
        }

        delivery.setSenderDetail(buildSenderDetail(restaurant));
        delivery.setReceiverDetail(buildReceiverDetail(address, customer));
        delivery.setAmount(order.getTotalAmount());
        return delivery;
    }

    private static Delivery.ContactDetail buildSenderDetail(Restaurant restaurant) {
        Delivery.ContactDetail sender = new Delivery.ContactDetail();
        sender.setName(restaurant.getRestaurantName());
        sender.setMobile(restaurant.getContact());
        Delivery.Address addr = new Delivery.Address();
        addr.setAddressLine1(restaurant.getAddress());
        addr.setPincode(restaurant.getPincode());
        addr.setCity(restaurant.getCity());
        addr.setState(restaurant.getState());
        addr.setLatitude(restaurant.getLocation().getLatitude());
        addr.setLongitude(restaurant.getLocation().getLongitude());
        sender.setAddress(addr);
        return sender;
    }

    private static Delivery.ContactDetail buildReceiverDetail(Address address, Customer customer) {
        Delivery.ContactDetail receiver = new Delivery.ContactDetail();
        receiver.setName(customer.getName());
        receiver.setMobile(customer.getMobile());
        Delivery.Address addr = new Delivery.Address();
        addr.setAddressLine1(address.getAddressOne());
        addr.setAddressLine2(address.getAddressTwo());
        addr.setPincode(address.getPincode());
        addr.setCity(address.getCity());
        addr.setState(address.getState());
        addr.setLatitude(address.getLocation().getLatitude());
        addr.setLongitude(address.getLocation().getLongitude());
        receiver.setAddress(addr);
        return receiver;
    }
}
