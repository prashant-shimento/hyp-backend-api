# Offer Engine — Requirements & Implementation Plan

## Background

The platform currently handles discounts in two places:
- `Restaurant.discountPercentage` — flat % applied to all items at checkout
- `Restaurant.discount` — conditional discount with min cart threshold (added 2026-04-27)
- `ReferralToken` / offer codes — user-level code-based discounts

These are insufficient for the full range of promotional mechanics needed. This document defines the complete offer engine to be built in a future sprint.

---

## Offer Types Required

| Type | Description | Example |
|---|---|---|
| `RESTAURANT_DISCOUNT` | % or flat off cart subtotal, optional min cart value | 10% off above ₹499 |
| `ITEM_DISCOUNT` | % or flat off specific item(s) | 20% off Burger |
| `FREE_DELIVERY` | Delivery charge waived entirely | Free delivery on first order |
| `DELIVERY_DISCOUNT` | % or flat reduction on delivery charge | 50% off delivery |
| `BOGO` | Buy item X, get item X (or Y) free | Buy 1 Pizza get 1 free |
| `FIRST_ORDER` | Applies on customer's very first order (platform-wide) | ₹100 off first order |
| `FIRST_N_ORDERS` | Applies on first N orders at a specific restaurant | 20% off first 3 orders |
| `COMBO_DISCOUNT` | Discount when specific items bought together | Burger + Fries = ₹199 |

---

## Offer Entity

```java
@Document(collection = "offers")
class Offer {
    String id;
    String restaurantId;          // null = platform-wide
    String partnerId;             // null = all partners
    String name;
    String description;
    String imageUrl;

    OfferType type;
    DiscountType discountType;    // PERCENTAGE, FLAT, FREE
    double discountValue;
    double maxDiscount;           // cap on discount, 0 = no cap
    double minCartValue;          // 0 = no minimum

    // Eligibility
    EligibilityType eligibility;  // ALL, NEW_USER, FIRST_N_ORDERS
    int maxOrderCount;            // for FIRST_N_ORDERS: applies when orderCount < maxOrderCount

    // Scope
    List<String> applicableItemIds;  // empty = all items

    // BOGO
    String bogoSourceItemId;      // buy this
    String bogoTargetItemId;      // get this free (null = same as source)

    // Limits
    String offerCode;             // null = auto-apply (no code needed)
    boolean autoApply;            // true = applied automatically without code
    int usageLimitTotal;          // 0 = unlimited
    int usageLimitPerCustomer;    // 0 = unlimited
    int usageCount;               // atomic counter — use findAndModify to increment

    boolean active;
    LocalDateTime startDate;
    LocalDateTime endDate;
    LocalDateTime createdAt;
}
```

---

## Enums

```java
enum OfferType {
    RESTAURANT_DISCOUNT,
    ITEM_DISCOUNT,
    FREE_DELIVERY,
    DELIVERY_DISCOUNT,
    BOGO,
    FIRST_ORDER,
    FIRST_N_ORDERS,
    COMBO_DISCOUNT
}

enum DiscountType {
    PERCENTAGE,
    FLAT,
    FREE
}

enum EligibilityType {
    ALL,
    NEW_USER,           // first order ever on platform
    FIRST_N_ORDERS      // first N orders at this restaurant
}
```

---

## Offer Engine

### Interface

```java
public interface OfferEngine {
    OfferResult evaluate(OfferContext context);
}

public class OfferContext {
    Order order;           // or OrderDto at validation time
    Customer customer;
    Restaurant restaurant;
    String offerCode;      // from order request, may be null
    int customerOrderCount;       // total orders on platform
    int customerRestaurantOrderCount; // orders at this restaurant
}

public class OfferResult {
    List<AppliedOffer> appliedOffers;
    double cartDiscount;
    double deliveryDiscount;
    Map<String, Double> itemDiscounts;   // itemId → discount amount
    List<String> freeItemIds;            // for BOGO
}

public class AppliedOffer {
    String offerId;
    String offerName;
    OfferType type;
    double discountAmount;
}
```

### Evaluation Steps

```
1. Load all active offers for restaurantId (including platform-wide where restaurantId = null)
   → filter: active = true, now between startDate and endDate

2. If offerCode present → load that specific offer and add to candidate list

3. For each candidate offer:
   a. Check date range
   b. Check minCartValue
   c. Check eligibility (ALL / NEW_USER / FIRST_N_ORDERS)
   d. Check usageLimitTotal (offer.usageCount < usageLimitTotal or unlimited)
   e. Check usageLimitPerCustomer (customerUsage < usageLimitPerCustomer or unlimited)
   f. Check applicableItemIds (order contains at least one applicable item)

4. Group eligible offers by discount target:
   - CART group:     RESTAURANT_DISCOUNT, FIRST_ORDER, FIRST_N_ORDERS
   - DELIVERY group: FREE_DELIVERY, DELIVERY_DISCOUNT
   - ITEM group:     ITEM_DISCOUNT, BOGO, COMBO_DISCOUNT

5. Stacking rules:
   - Within each group: apply the single best offer (highest discount value)
   - CART + DELIVERY groups CAN coexist
   - ITEM group can coexist with CART/DELIVERY
   - Two CART offers cannot stack

6. Apply selected offers → produce OfferResult
```

---

## Integration Points

### Order Creation (`OrderService.create`)

```java
OfferContext ctx = OfferContext.builder()
    .order(order)
    .customer(customer)
    .restaurant(restaurant)
    .offerCode(orderDto.getOfferCode())
    .customerOrderCount(orderRepository.countByCustomerId(customer.getId()))
    .customerRestaurantOrderCount(orderRepository.countByCustomerIdAndRestaurantId(...))
    .build();

OfferResult result = offerEngine.evaluate(ctx);
order.setDiscountAmount(result.getCartDiscount());
order.setDeliveryCharge(deliveryCharge - result.getDeliveryDiscount());
order.setAppliedOffers(result.getAppliedOffers());
// BOGO: inject free items into order lines
```

### Usage Tracking

Use MongoDB `findAndModify` with `$inc` on `usageCount` — atomic, no race conditions.
Track per-customer usage in a separate `offer_usage` collection:

```java
@Document(collection = "offer_usage")
class OfferUsage {
    String offerId;
    String customerId;
    String orderId;
    LocalDateTime usedAt;
}
```

---

## APIs Required

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/offer` | Create offer (ops) |
| `GET` | `/offer/{restaurantId}` | List offers for restaurant |
| `GET` | `/offer/{restaurantId}/active` | Active offers visible to customer |
| `PATCH` | `/offer/{offerId}` | Update offer |
| `DELETE` | `/offer/{offerId}` | Deactivate offer |
| `GET` | `/offer/validate?code=X&restaurantId=Y&cartValue=Z` | Validate offer code before checkout |

---

## MongoDB Indexes

```
offers: { restaurantId: 1, active: 1, startDate: 1, endDate: 1 }
offers: { offerCode: 1 }  (sparse)
offer_usage: { offerId: 1, customerId: 1 }
```

---

## Implementation Phases

### Phase 1 — Foundation
- `Offer` entity, `OfferRepository`, `OfferService` (CRUD)
- `OfferEngine` with `RESTAURANT_DISCOUNT`, `FREE_DELIVERY`, `FIRST_ORDER`, `FIRST_N_ORDERS`
- Wire into `OrderValidator`
- Basic CRUD APIs

### Phase 2 — Item & Delivery
- `ITEM_DISCOUNT`, `DELIVERY_DISCOUNT`
- `applicableItemIds` scoping
- Per-customer usage tracking (`offer_usage` collection)
- Usage limit enforcement with atomic increment

### Phase 3 — Advanced
- `BOGO` — free item injection into order lines
- `COMBO_DISCOUNT` — multi-item combination detection
- Offer stacking rule configuration per restaurant
- Customer-facing offer listing API

---

## Backward Compatibility

- `Restaurant.discountPercentage` — keep as-is, applied as Level 2 in existing price calculation
- `Restaurant.discount` (new simple field, 2026-04-27) — applied as a conditional check in `OrderValidator`
- Offer engine runs as Level 3 on top of both existing mechanisms
- No existing order data is affected
