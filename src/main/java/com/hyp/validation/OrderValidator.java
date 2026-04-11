package com.hyp.validation;

import static com.hyp.util.CommonUtils.roundToTwoDecimal;
import static com.hyp.util.ValidationUtils.isWithinDeliveryHours;

import com.hyp.dto.OrderDto;
import com.hyp.entity.*;
import com.hyp.enums.OfferType;
import com.hyp.enums.OrderType;
import com.hyp.enums.PartnerType;
import com.hyp.exception.EntityNotFoundException;
import com.hyp.exception.ValidationException;
import com.hyp.model.PreOrder;
import com.hyp.repository.DeliveryQuoteRepository;
import com.hyp.repository.OrderRepository;
import com.hyp.service.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Order validation with FULLY PARALLELIZED data fetching.
 *
 * All DB calls happen in Phase 1 (parallel), Phase 2 is pure CPU validation.
 * This reduces latency from ~650ms to ~450ms by eliminating sequential DB waits.
 */
@Slf4j
@Component
public class OrderValidator {

    /** Dedicated pool for Phase 1 parallel DB fetches — sized for I/O-bound work. */
    private static final ExecutorService DB_FETCH_POOL = Executors.newFixedThreadPool(12);

    public record PriceWarning(String type, String message) {}

    public record PriceCalculation(
            double itemTotal, double taxTotal, double itemDiscount, double restaurantDiscount, double offerAmount) {}

    public record OrderValidationResult(
            Restaurant restaurant,
            Customer customer,
            double itemTotalAmount,
            double totalAmount,
            double grandTotalAmount,
            double taxTotalAmount,
            double discountAmount,
            double deliveryCharge,
            ReferralToken referralToken,
            List<PriceWarning> priceWarnings,
            boolean serverCorrectionApplied) {}

    @Autowired
    private RestaurantService restaurantService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private PartnerService partnerService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private AddonItemService addonItemService;

    @Autowired
    private VariationService variationService;

    @Autowired
    private TaxService taxService;

    @Autowired
    private AddressService addressService;

    @Autowired
    private LocationService locationService;

    @Autowired
    private OfferService offerService;

    @Autowired
    private OfferUsageService offerUsageService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ReferralTokenService referralTokenService;

    @Autowired
    private DeliveryQuoteRepository deliveryQuoteRepository;

    @Autowired
    private CacheService cacheService;

    public OrderValidationResult validate(OrderDto orderDto) throws Exception {
        long startTime = System.currentTimeMillis();
        List<PriceWarning> warnings = new ArrayList<>();

        /* ═══════════════ PHASE 1 – PARALLEL DB FETCH ═══════════════ */

        CompletableFuture<Restaurant> restaurantFuture =
                timed("restaurant", () -> restaurantService.findById(orderDto.getRestaurantId()));

        CompletableFuture<Customer> customerFuture =
                timed("customer", () -> customerService.findById(orderDto.getCustomerId()));

        CompletableFuture<Partner> partnerFuture = timed("partner", () -> {
            if (orderDto.getPartnerId() != null) {
                return partnerService.findById(orderDto.getPartnerId());
            }
            return partnerService.findPartnersByRestaurantId(orderDto.getRestaurantId(), PartnerType.RESTAURANT);
        });

        // ── Catalog batch lookups ─────────────────────────────────────────────
        List<String> itemIds = new ArrayList<>();
        List<String> variationIds = new ArrayList<>();
        List<String> addonIds = new ArrayList<>();
        List<String> taxIds = new ArrayList<>();

        for (OrderDto.OrderItem oi : orderDto.getOrderItems()) {
            if (oi.getVariationId() != null) variationIds.add(oi.getId());
            else itemIds.add(oi.getId());

            if (oi.getOrderAddonItems() != null) oi.getOrderAddonItems().forEach(a -> addonIds.add(a.getAddonItemId()));
            if (oi.getOrderItemTax() != null) oi.getOrderItemTax().forEach(t -> taxIds.add(t.getId()));
        }
        if (orderDto.getOrderTax() != null) orderDto.getOrderTax().forEach(t -> taxIds.add(t.getId()));

        CompletableFuture<Map<String, Item>> itemsFuture =
                timed("items(" + itemIds.size() + ")", () -> itemService.findAllByIdIn(itemIds).stream()
                        .collect(Collectors.toMap(Item::getId, i -> i)));
        CompletableFuture<Map<String, Variation>> variationsFuture = timed(
                "variations(" + variationIds.size() + ")", () -> variationService.findAllByIdIn(variationIds).stream()
                        .collect(Collectors.toMap(Variation::getId, v -> v)));
        CompletableFuture<Map<String, AddonItem>> addonsFuture =
                timed("addons(" + addonIds.size() + ")", () -> addonItemService.findAllByIdIn(addonIds).stream()
                        .collect(Collectors.toMap(AddonItem::getId, a -> a)));
        CompletableFuture<Map<String, Tax>> taxesFuture =
                timed("taxes(" + taxIds.size() + ")", () -> taxService.findAllByIdIn(taxIds).stream()
                        .collect(Collectors.toMap(Tax::getId, t -> t)));

        // ── Address (only if delivery order with addressId) ───────────────────
        String addressId = null;
        if (orderDto.getDeliveryDetails() != null
                && orderDto.getDeliveryDetails().getAddressId() != null) {
            addressId = orderDto.getDeliveryDetails().getAddressId();
        }
        final String finalAddressId = addressId;
        CompletableFuture<Address> addressFuture = timed("address", () -> {
            if (finalAddressId != null) {
                return addressService.findById(finalAddressId);
            }
            return null;
        });

        // ── Offer + usage count (only if offer code present) ──────────────────
        String offerCode = orderDto.getOfferCode();
        CompletableFuture<Offer> offerFuture = timed("offer", () -> {
            if (offerCode != null && !offerCode.isBlank()) {
                return offerService.findByOfferCode(offerCode);
            }
            return null;
        });

        CompletableFuture<Long> orderCountFuture = timed("orderCount", () -> {
            if (offerCode != null && !offerCode.isBlank()) {
                return orderRepository.countByCustomerIdAndStatus(orderDto.getCustomerId(), "DELIVERED");
            }
            return 0L;
        });

        CompletableFuture<Integer> offerUsageCountFuture = timed("offerUsage", () -> {
            if (offerCode != null && !offerCode.isBlank()) {
                return offerUsageService.getUsageCount(offerCode, orderDto.getCustomerId());
            }
            return 0;
        });

        // ── Referral token (only if token present) ────────────────────────────
        String referralTokenStr = orderDto.getReferralToken();
        CompletableFuture<ReferralToken> referralTokenFuture = timed("referralToken", () -> {
            if (referralTokenStr != null && !referralTokenStr.isBlank()) {
                return referralTokenService.validateTokenForOrder(referralTokenStr);
            }
            return null;
        });

        // ── Delivery quote (only if quoteId present) ─────────────────────────
        String deliveryQuoteId = null;
        if (orderDto.getDeliveryDetails() != null
                && orderDto.getDeliveryDetails().getDeliveryQuoteId() != null) {
            deliveryQuoteId = orderDto.getDeliveryDetails().getDeliveryQuoteId();
        }
        final String finalDeliveryQuoteId = deliveryQuoteId;
        CompletableFuture<DeliveryQuoteRecord> deliveryQuoteFuture = timed("deliveryQuote", () -> {
            if (finalDeliveryQuoteId != null) {
                return deliveryQuoteRepository.findById(finalDeliveryQuoteId).orElse(null);
            }
            return null;
        });

        // ── Wait for ALL parallel fetches ─────────────────────────────────────
        CompletableFuture.allOf(
                        restaurantFuture,
                        customerFuture,
                        partnerFuture,
                        itemsFuture,
                        variationsFuture,
                        addonsFuture,
                        taxesFuture,
                        addressFuture,
                        offerFuture,
                        orderCountFuture,
                        offerUsageCountFuture,
                        referralTokenFuture,
                        deliveryQuoteFuture)
                .join();

        // ── Extract results ───────────────────────────────────────────────────
        Restaurant restaurant = restaurantFuture.get();
        Customer customer = customerFuture.get();
        Partner partner = partnerFuture.get();
        Map<String, Item> itemMap = itemsFuture.get();
        Map<String, Variation> variationMap = variationsFuture.get();
        Map<String, AddonItem> addonMap = addonsFuture.get();
        Map<String, Tax> taxMap = taxesFuture.get();
        Address address = addressFuture.get();
        Offer offer = offerFuture.get();
        long paidOrderCount = orderCountFuture.get();
        int offerUsageCount = offerUsageCountFuture.get();
        ReferralToken referralToken = referralTokenFuture.get();
        DeliveryQuoteRecord deliveryQuoteRecord = deliveryQuoteFuture.get();

        log.info("Phase 1 (parallel lookups) completed in {}ms", System.currentTimeMillis() - startTime);

        // ══════════════════════════════════════════════════════════════════════
        // PHASE 2: PURE CPU VALIDATION (no more DB calls)
        // ══════════════════════════════════════════════════════════════════════
        long validationStart = System.currentTimeMillis();

        validateEntities(restaurant, customer, partner, orderDto);

        // resolve default partner once and write back onto the DTO
        if (orderDto.getPartnerId() == null) {
            orderDto.setPartnerId(partner.getId());
        }

        // pre-order window
        boolean isPreOrder = Boolean.TRUE.equals(orderDto.getPreOrder());
        PreOrder preOrderConfig = getPreOrder(orderDto, restaurant, isPreOrder);
        if (isPreOrder) {
            validatePreOrderTime(orderDto.getPreOrderDateTime(), preOrderConfig, restaurant.getDeliveryHours());
        }

        // delivery-address reachability (using pre-fetched address)
        if (OrderType.fromCode(orderDto.getOrderType()) == OrderType.H) {
            validateDeliveryAddress(orderDto, restaurant, address);
        }

        // server-side price calculation with three-level discount distribution:
        //   Level 1 – item-level offer  (FLAT / PERCENTAGE / FIXED_PRICE on the item)
        //   Level 2 – restaurant discount (percentage applied to ALL items after level-1)
        //   Level 3 – customer offer code (PERCENTAGE per-item; FLAT/FIXED proportionally)
        // Tax is computed on the fully-discounted taxable base for each item.
        double restaurantDiscountPercentage =
                restaurant.getDiscountPercentage() != null ? restaurant.getDiscountPercentage() : 0.0;

        // Validate offer eligibility first (throws if invalid, before any price computation)
        checkOfferValidity(orderDto, offer, paidOrderCount, offerUsageCount);

        PriceCalculation priceCalculation = validatePricesAndCalculate(
                orderDto, itemMap, variationMap, addonMap, taxMap, warnings, restaurantDiscountPercentage, offer);
        double itemTotalAmount = priceCalculation.itemTotal();
        double serverTaxTotal = priceCalculation.taxTotal();
        double totalItemLevelDiscount = priceCalculation.itemDiscount();
        double restaurantDiscount = priceCalculation.restaurantDiscount();
        double appliedOfferAmount = priceCalculation.offerAmount();

        // Save offer usage after all price validation passes
        if (offer != null && appliedOfferAmount > 0 && offerCode != null && !offerCode.isBlank()) {
            saveOfferUsage(offerCode, orderDto.getCustomerId(), orderDto.getPartnerId(), offerUsageCount + 1);
        }

        // ── Feature flag: server correction per partner ─────────────────────
        // Mismatch detection (warnings) is ALWAYS active for all partners.
        // Value correction (overriding client values) is only applied for
        // partners that are enabled via the server correction feature flag.
        boolean applyCorrection = cacheService.isServerCorrectionEnabled(orderDto.getPartnerId());

        double clientDeliveryCharge = roundToTwoDecimal(orderDto.getDeliveryCharge());
        double finalDeliveryCharge = clientDeliveryCharge;

        // ── Delivery charge validation against saved quote ────────────────────
        if (deliveryQuoteRecord != null
                && deliveryQuoteRecord.getNetwork() != null
                && deliveryQuoteRecord.getNetwork().getQuote() != null) {
            double quotedDeliveryCharge = roundToTwoDecimal(
                    deliveryQuoteRecord.getNetwork().getQuote().getPrice());

            Double totalDeliveryShare = restaurant.getTotalDeliveryShare();
            if (totalDeliveryShare == null || totalDeliveryShare <= 0) {
                double restShare =
                        restaurant.getRestaurantDeliveryShare() != null ? restaurant.getRestaurantDeliveryShare() : 0.0;
                double platShare =
                        restaurant.getPlatformDeliveryShare() != null ? restaurant.getPlatformDeliveryShare() : 0.0;
                totalDeliveryShare = restShare + platShare;
            }
            double discountedDeliveryCharge = quotedDeliveryCharge;
            if (totalDeliveryShare > 0) {
                double discount = (quotedDeliveryCharge * totalDeliveryShare) / 100;
                discountedDeliveryCharge = roundToTwoDecimal(quotedDeliveryCharge - discount);
                log.info(
                        "Delivery charge after applying totalDeliveryShare discount of {}% is {}",
                        totalDeliveryShare, discountedDeliveryCharge);
            }
            if (clientDeliveryCharge != discountedDeliveryCharge) {
                warnings.add(new PriceWarning(
                        "DELIVERY_CHARGE_MISMATCH",
                        "client=" + clientDeliveryCharge + " expected="
                                + discountedDeliveryCharge + " quoteId="
                                + deliveryQuoteRecord.getId()));

                finalDeliveryCharge = discountedDeliveryCharge;
            } else {
                finalDeliveryCharge = discountedDeliveryCharge;
            }
        }

        if (applyCorrection) {
            orderDto.setDeliveryCharge(finalDeliveryCharge);
        }

        // ── Delivery charge tax (dcTaxAmount) ────────────────────────────────
        double serverDcTaxAmount = 0.0;
        if (Integer.valueOf(1).equals(restaurant.getCalculateTaxOnDelivery())
                && restaurant.getTax() != null
                && restaurant.getTax().getDcTaxesId() != null) {
            Tax dcTax = taxMap.get(restaurant.getTax().getDcTaxesId());
            if (dcTax == null) {
                dcTax = taxService.findById(restaurant.getTax().getDcTaxesId());
            }
            if (dcTax != null) {
                double dcTaxRate = Double.parseDouble(dcTax.getTax());
                serverDcTaxAmount = roundToTwoDecimal((finalDeliveryCharge * dcTaxRate) / 100);
            }
        }
        double clientDcTaxAmount =
                orderDto.getDcTaxAmount() != null ? roundToTwoDecimal(orderDto.getDcTaxAmount()) : 0.0;
        if (clientDcTaxAmount != serverDcTaxAmount) {
            warnings.add(new PriceWarning(
                    "DC_TAX_MISMATCH", "client=" + clientDcTaxAmount + " server=" + serverDcTaxAmount));
        }
        if (applyCorrection) {
            orderDto.setDcTaxAmount(serverDcTaxAmount);
        }

        // Total discount = item-level + restaurant + offer
        double totalDiscount = roundToTwoDecimal(totalItemLevelDiscount + restaurantDiscount + appliedOfferAmount);

        // Security: Override client discount amount with server-calculated value
        double clientDiscountAmount = roundToTwoDecimal(orderDto.getDiscountAmount());
        if (clientDiscountAmount != totalDiscount) {
            warnings.add(new PriceWarning(
                    "DISCOUNT_MISMATCH",
                    "client=" + clientDiscountAmount + " server=" + totalDiscount + " (itemLevel="
                            + totalItemLevelDiscount + " restaurant=" + restaurantDiscount + " offer="
                            + appliedOfferAmount + ")"));
        }
        if (applyCorrection) {
            orderDto.setDiscountAmount(totalDiscount);
        }

        // ── Final amount calculations ─────────────────────────────────────────
        // totalAmount = itemTotalAmount + taxTotalAmount - discountAmount
        double serverTotalAmount = roundToTwoDecimal(itemTotalAmount + serverTaxTotal - totalDiscount);
        if (serverTotalAmount < 0) serverTotalAmount = 0;

        if (roundToTwoDecimal(orderDto.getTotalAmount()) != serverTotalAmount) {
            warnings.add(new PriceWarning(
                    "TOTAL_AMOUNT_MISMATCH", "client=" + orderDto.getTotalAmount() + " server=" + serverTotalAmount));
        }
        if (applyCorrection) {
            orderDto.setTotalAmount(serverTotalAmount);
        }

        // ── Packaging charge validation ─────────────────────────
        // Packaging charge is per-item × quantity, summed across all order items.
        double perItemPackagingRate = Optional.ofNullable(restaurant.getPackagingCharge())
                .filter(s -> !s.isBlank())
                .map(Double::parseDouble)
                .orElse(0.0);

        double serverPackagingCharge = 0.0;
        for (OrderDto.OrderItem oi : orderDto.getOrderItems()) {
            serverPackagingCharge += perItemPackagingRate * oi.getQuantity();
        }
        serverPackagingCharge = roundToTwoDecimal(serverPackagingCharge);

        double clientPackagingCharge = roundToTwoDecimal(orderDto.getPackagingCharge());

        if (clientPackagingCharge != serverPackagingCharge) {
            warnings.add(new PriceWarning(
                    "PACKAGING_CHARGE_MISMATCH",
                    "client=" + clientPackagingCharge + " server=" + serverPackagingCharge));
        }

        if (applyCorrection) {
            orderDto.setPackagingCharge(serverPackagingCharge);
        }

        // ── Packaging charge tax (pcTaxAmount) ───────────────────────────────
        double serverPcTaxAmount = 0.0;
        if (Integer.valueOf(1).equals(restaurant.getCalculateTaxOnPacking())
                && restaurant.getTax() != null
                && restaurant.getTax().getPcTaxesId() != null) {
            Tax pcTax = taxMap.get(restaurant.getTax().getPcTaxesId());
            if (pcTax == null) {
                pcTax = taxService.findById(restaurant.getTax().getPcTaxesId());
            }
            if (pcTax != null) {
                double pcTaxRate = Double.parseDouble(pcTax.getTax());
                serverPcTaxAmount = roundToTwoDecimal((serverPackagingCharge * pcTaxRate) / 100);
            }
        }
        double clientPcTaxAmount =
                orderDto.getPcTaxAmount() != null ? roundToTwoDecimal(orderDto.getPcTaxAmount()) : 0.0;
        if (clientPcTaxAmount != serverPcTaxAmount) {
            warnings.add(new PriceWarning(
                    "PC_TAX_MISMATCH", "client=" + clientPcTaxAmount + " server=" + serverPcTaxAmount));
        }
        if (applyCorrection) {
            orderDto.setPcTaxAmount(serverPcTaxAmount);
        }

        double serverGrandTotal = roundToTwoDecimal(itemTotalAmount
                + serverTaxTotal
                + finalDeliveryCharge
                + serverDcTaxAmount
                + serverPackagingCharge
                + serverPcTaxAmount
                - totalDiscount);
        if (serverGrandTotal < 0) serverGrandTotal = 0;

        if (roundToTwoDecimal(orderDto.getGrandTotalAmount()) != serverGrandTotal) {
            warnings.add(new PriceWarning(
                    "GRAND_TOTAL_MISMATCH",
                    "client=" + orderDto.getGrandTotalAmount() + " server=" + serverGrandTotal));
        }
        // itemTotalAmount is always set — it is server-computed, not a client override.
        orderDto.setItemTotalAmount(itemTotalAmount);
        if (applyCorrection) {
            orderDto.setGrandTotalAmount(serverGrandTotal);
        }

        // Log all price mismatches as warnings (always, regardless of correction flag)
        if (!warnings.isEmpty()) {
            log.warn(
                    "Price mismatches for restaurantId={} (correction={}): {}",
                    orderDto.getRestaurantId(),
                    applyCorrection,
                    warnings);
        }

        log.info("Phase 2 (validations) completed in {}ms", System.currentTimeMillis() - validationStart);

        return new OrderValidationResult(
                restaurant,
                customer,
                itemTotalAmount,
                serverTotalAmount,
                serverGrandTotal,
                serverTaxTotal,
                totalDiscount,
                finalDeliveryCharge,
                referralToken,
                warnings,
                applyCorrection);
    }

    // ─── individual validation gates (kept public for unit-test granularity) ─

    public void validateEntities(Restaurant restaurant, Customer customer, Partner partner, OrderDto orderDto)
            throws ValidationException, EntityNotFoundException {

        if (restaurant == null) {
            throw new EntityNotFoundException(Restaurant.class.getSimpleName(), orderDto.getRestaurantId());
        }
        if (customer == null) {
            throw new EntityNotFoundException(Customer.class.getSimpleName(), orderDto.getCustomerId());
        }
        if (partner == null && orderDto.getPartnerId() != null) {
            throw new EntityNotFoundException(Partner.class.getSimpleName(), orderDto.getPartnerId());
        }
        if (!restaurant.isServiceable()) {
            throw new ValidationException("Restaurant is not serviceable");
        }
        if (!restaurant.isActive()) {
            throw new ValidationException("Restaurant is not active");
        }
        if (!isWithinDeliveryHours(restaurant.getDeliveryHours())) {
            throw new ValidationException("Order cannot be processed: Outside delivery hours");
        }
    }

    /**
     * Validate delivery address using PRE-FETCHED address (no DB call here).
     */
    public void validateDeliveryAddress(OrderDto orderDto, Restaurant restaurant, Address address)
            throws ValidationException, EntityNotFoundException {

        if (orderDto.getDeliveryDetails() != null
                && orderDto.getDeliveryDetails().getAddressId() != null) {
            if (address == null) {
                throw new EntityNotFoundException(
                        Address.class.getSimpleName(),
                        orderDto.getDeliveryDetails().getAddressId());
            }
            if (!locationService.isLocationDeliverable(
                    address.getLocation().getLatitude(),
                    address.getLocation().getLongitude(),
                    restaurant.getLocation().getLatitude(),
                    restaurant.getLocation().getLongitude(),
                    restaurant.getDeliveryRadius())) {
                throw new ValidationException("Location not deliverable");
            }
        } else {
            if (orderDto.getSeat() == null || orderDto.getScreen() == null) {
                throw new ValidationException("Delivery details missing: Seat and Screen required");
            }
        }
    }

    public PriceCalculation validatePricesAndCalculate(
            OrderDto orderDto,
            Map<String, Item> itemMap,
            Map<String, Variation> variationMap,
            Map<String, AddonItem> addonMap,
            Map<String, Tax> taxMap,
            List<PriceWarning> warnings,
            double restaurantDiscountPercentage,
            Offer offer)
            throws ValidationException {

        // ══ PASS 1 ══════════════════════════════════════════════════════════
        // Verify catalog prices, apply level-1 (item offer) and level-2
        // (restaurant) discounts per item.  Accumulate the post-restaurant
        // base for each item — needed to distribute the level-3 offer.
        record ItemCalc(OrderDto.OrderItem oi, double lineTotal, double postRestBase) {}
        List<ItemCalc> itemCalcs = new ArrayList<>();

        double itemTotalAmount = 0.0;
        double totalItemLevelDiscount = 0.0;
        double totalRestaurantDiscount = 0.0;
        double totalPostRestBase = 0.0;

        for (OrderDto.OrderItem oi : orderDto.getOrderItems()) {
            boolean isVariation = oi.getVariationId() != null;
            double basePrice;
            Item item = null;

            if (isVariation) {
                Variation v = variationMap.get(oi.getId());
                if (v == null) throw new ValidationException("Variation not found: " + oi.getId());
                basePrice = roundToTwoDecimal(Double.parseDouble(v.getPrice()));
            } else {
                item = itemMap.get(oi.getId());
                if (item == null) throw new ValidationException("Item not found: " + oi.getId());
                basePrice = roundToTwoDecimal(Double.parseDouble(item.getPrice()));
                oi.setItemAttribute(item.getItemAttributeId());
            }
            if (roundToTwoDecimal(oi.getPrice()) != basePrice) {
                warnings.add(new PriceWarning(
                        "ITEM_PRICE_MISMATCH",
                        "Item " + oi.getId() + " client=" + oi.getPrice() + " server=" + basePrice));
            }
            oi.setPrice(basePrice);

            double lineTotal = roundToTwoDecimal(basePrice * oi.getQuantity());

            if (oi.getOrderAddonItems() != null) {
                for (OrderDto.OrderAddonItem addon : oi.getOrderAddonItems()) {
                    AddonItem dbAddon = addonMap.get(addon.getAddonItemId());
                    if (dbAddon == null) throw new ValidationException("Addon not found: " + addon.getAddonItemId());
                    double addonPrice = roundToTwoDecimal(Double.parseDouble(dbAddon.getAddonItemPrice()));
                    if (roundToTwoDecimal(addon.getPrice()) != addonPrice) {
                        warnings.add(new PriceWarning(
                                "ADDON_PRICE_MISMATCH",
                                "Addon " + addon.getAddonItemId() + " client=" + addon.getPrice() + " server="
                                        + addonPrice));
                    }
                    addon.setPrice(addonPrice);
                    lineTotal = roundToTwoDecimal(lineTotal + (addonPrice * addon.getQuantity()));
                }
            }
            oi.setFinalPrice(lineTotal);

            // Level 1 – item-level offer discount
            double serverItemDiscount = calculateItemDiscount(item, oi.getQuantity(), lineTotal);
            double clientItemDiscount = oi.getItemDiscount() != null ? roundToTwoDecimal(oi.getItemDiscount()) : 0.0;
            if (clientItemDiscount != serverItemDiscount) {
                warnings.add(new PriceWarning(
                        "ITEM_DISCOUNT_MISMATCH",
                        "Item " + oi.getId() + " client=" + clientItemDiscount + " server=" + serverItemDiscount));
            }
            oi.setItemDiscount(serverItemDiscount);

            // Level 2 – restaurant discount applied to ALL items after level-1
            double postItemBase = roundToTwoDecimal(lineTotal - serverItemDiscount);
            double restDiscountForItem = roundToTwoDecimal((postItemBase * restaurantDiscountPercentage) / 100.0);
            double postRestBase = roundToTwoDecimal(postItemBase - restDiscountForItem);

            itemCalcs.add(new ItemCalc(oi, lineTotal, postRestBase));
            itemTotalAmount = roundToTwoDecimal(itemTotalAmount + lineTotal);
            totalItemLevelDiscount = roundToTwoDecimal(totalItemLevelDiscount + serverItemDiscount);
            totalRestaurantDiscount = roundToTwoDecimal(totalRestaurantDiscount + restDiscountForItem);
            totalPostRestBase = roundToTwoDecimal(totalPostRestBase + postRestBase);
        }

        // ── Compute total level-3 offer discount amount ───────────────────────
        // PERCENTAGE: the rate applied per item; total = rate × totalPostRestBase
        // FLAT / FIXED: an absolute amount, capped at totalPostRestBase
        double offerDiscountTotal = 0.0;
        if (offer != null && totalPostRestBase > 0) {
            double discountValue = offer.getDiscountValue();
            if ("PERCENTAGE".equalsIgnoreCase(offer.getOfferType().toString())) {
                offerDiscountTotal = roundToTwoDecimal((totalPostRestBase * discountValue) / 100.0);
            } else {
                offerDiscountTotal = roundToTwoDecimal(Math.min(discountValue, totalPostRestBase));
            }
        }
        final double finalOfferTotal = offerDiscountTotal;
        final double finalTotalPostRestBase = totalPostRestBase;

        // ══ PASS 2 ══════════════════════════════════════════════════════════
        // Distribute level-3 offer per item and compute tax on the fully-
        // discounted taxable base (level-1 + level-2 + level-3 applied).
        double taxTotalAmount = 0.0;
        Map<String, double[]> orderTaxAccumulator = new LinkedHashMap<>();
        Map<String, String[]> orderTaxInfo = new LinkedHashMap<>();

        for (ItemCalc calc : itemCalcs) {
            OrderDto.OrderItem oi = calc.oi();
            double postRestBase = calc.postRestBase();

            // Level 3 – customer offer distributed to this item
            double offerForItem = 0.0;
            if (finalOfferTotal > 0 && finalTotalPostRestBase > 0) {
                if ("PERCENTAGE".equalsIgnoreCase(offer.getOfferType().toString())) {
                    // PERCENTAGE: apply the same rate to each item's base
                    offerForItem = roundToTwoDecimal((postRestBase * offer.getDiscountValue()) / 100.0);
                } else {
                    // FLAT / FIXED: share proportionally by this item's weight
                    offerForItem = roundToTwoDecimal((postRestBase / finalTotalPostRestBase) * finalOfferTotal);
                }
            }

            double taxableBase = roundToTwoDecimal(postRestBase - offerForItem);
            if (taxableBase < 0) taxableBase = 0;

            // Tax on fully-discounted base (all three levels applied)
            if (oi.getOrderItemTax() != null) {
                for (OrderDto.OrderItemTax it : oi.getOrderItemTax()) {
                    Tax tax = taxMap.get(it.getId());
                    if (tax == null) throw new ValidationException("Tax not found: " + it.getId());
                    double taxPercent = Double.parseDouble(tax.getTax());
                    double expectedTax = roundToTwoDecimal((taxableBase * taxPercent) / 100);
                    if (roundToTwoDecimal(it.getAmount()) != expectedTax) {
                        warnings.add(new PriceWarning(
                                "ITEM_TAX_MISMATCH",
                                tax.getTaxName() + " client=" + it.getAmount() + " server=" + expectedTax));
                    }
                    it.setAmount(expectedTax);
                    taxTotalAmount = roundToTwoDecimal(taxTotalAmount + expectedTax);

                    orderTaxAccumulator.merge(it.getId(), new double[] {expectedTax, taxableBase}, (a, b) -> {
                        a[0] += b[0];
                        a[1] += b[1];
                        return a;
                    });
                    orderTaxInfo.putIfAbsent(
                            it.getId(), new String[] {tax.getTaxName(), tax.getTaxType(), tax.getTax()});
                }
            }
        }

        List<OrderDto.OrderTax> serverOrderTax = new ArrayList<>();
        for (Map.Entry<String, double[]> entry : orderTaxAccumulator.entrySet()) {
            String taxId = entry.getKey();
            double[] values = entry.getValue();
            String[] info = orderTaxInfo.get(taxId);

            OrderDto.OrderTax ot = new OrderDto.OrderTax();
            ot.setId(taxId);
            ot.setTitle(info != null ? info[0] : null);
            ot.setType(info != null ? info[1] : null);
            ot.setPrice(Double.valueOf(info != null ? info[2] : null));
            ot.setTax(roundToTwoDecimal(values[0]));
            ot.setRestaurantLiableAmt(roundToTwoDecimal(values[0]));
            serverOrderTax.add(ot);
        }
        orderDto.setOrderTax(serverOrderTax);

        if (roundToTwoDecimal(orderDto.getTaxAmount()) != taxTotalAmount) {
            warnings.add(new PriceWarning(
                    "TAX_TOTAL_MISMATCH", "client=" + orderDto.getTaxAmount() + " server=" + taxTotalAmount));
        }
        orderDto.setTaxAmount(taxTotalAmount);

        return new PriceCalculation(
                itemTotalAmount, taxTotalAmount, totalItemLevelDiscount, totalRestaurantDiscount, offerDiscountTotal);
    }

    /**
     * Calculate item-level discount from the Item entity's offer fields.
     * Only applies to non-variation items (variations don't have offer fields).
     */
    private double calculateItemDiscount(Item item, int quantity, double lineTotal) {
        if (item == null) return 0.0; // variation items — no item-level discount

        // Check if offer is enabled
        if (!Boolean.TRUE.equals(item.getOfferEnabled())) return 0.0;
        if (item.getOfferType() == null || item.getOfferValue() == null || item.getOfferValue() <= 0) return 0.0;

        double discount;
        if (item.getOfferType() == OfferType.PERCENTAGE) {
            discount = roundToTwoDecimal((lineTotal * item.getOfferValue()) / 100);
        } else if (item.getOfferType() == OfferType.FLAT) {
            discount = roundToTwoDecimal(item.getOfferValue());
        } else if (item.getOfferType() == OfferType.FIXED_PRICE) {
            // FIXED_PRICE means the item is sold at offerValue; discount is the difference
            double fixedTotal = roundToTwoDecimal(item.getOfferValue() * quantity);
            discount = roundToTwoDecimal(Math.max(0, lineTotal - fixedTotal));
        } else {
            discount = 0.0;
        }

        // Discount cannot exceed line total
        return Math.min(discount, lineTotal);
    }

    /**
     * Validates offer eligibility only — throws if invalid, returns silently if no offer code.
     * Amount computation and usage saving are handled elsewhere.
     */
    private void checkOfferValidity(OrderDto orderDto, Offer offer, long paidOrderCount, int offerUsageCount)
            throws Exception {

        if (orderDto.getOfferCode() == null || orderDto.getOfferCode().isBlank()) return;

        if (offer == null) throw new Exception("Offer not found: " + orderDto.getOfferCode());
        if (!Boolean.TRUE.equals(offer.getIsActive()))
            throw new Exception("Offer is not active: " + orderDto.getOfferCode());

        ZoneId zone = ZoneId.of("Asia/Kolkata");
        ZonedDateTime now = ZonedDateTime.now(zone);

        if (offer.getStartDate() != null && now.isBefore(offer.getStartDate().atZone(zone)))
            throw new Exception("Offer not started yet");
        if (offer.getEndDate() != null && now.isAfter(offer.getEndDate().atZone(zone)))
            throw new Exception("Offer expired");

        if (offer.getPartnerId() != null && !offer.getPartnerId().equals(orderDto.getPartnerId()))
            throw new Exception("Offer not valid for this partner");
        if (offer.getRestaurantId() != null && !offer.getRestaurantId().equals(orderDto.getRestaurantId()))
            throw new Exception("Offer not valid for this restaurant");

        int maxLimit =
                offer.getMaximumRedemptionLimit() != null ? Integer.parseInt(offer.getMaximumRedemptionLimit()) : 0;
        if (offerUsageCount >= maxLimit || paidOrderCount >= maxLimit)
            throw new Exception("Offer usage limit exceeded. Maximum " + maxLimit + " attempts allowed.");
    }

    /**
     * Validate and apply offer using PRE-FETCHED offer + counts (no DB calls here).
     * @deprecated Main flow now uses {@link #checkOfferValidity} + per-item distribution
     *             inside {@link #validatePricesAndCalculate}. Kept for unit-test compatibility.
     */
    public double validateAndApplyOffer(
            OrderDto orderDto, double itemTotalAmount, Offer offer, long paidOrderCount, int offerUsageCount)
            throws Exception {

        if (orderDto.getOfferCode() == null || orderDto.getOfferCode().isBlank()) {
            return 0d;
        }

        if (offer == null) {
            throw new Exception("Offer not found: " + orderDto.getOfferCode());
        }

        if (!Boolean.TRUE.equals(offer.getIsActive())) {
            throw new Exception("Offer is not active: " + orderDto.getOfferCode());
        }

        ZoneId zone = ZoneId.of("Asia/Kolkata");
        ZonedDateTime now = ZonedDateTime.now(zone);

        if (offer.getStartDate() != null) {
            ZonedDateTime start = offer.getStartDate().atZone(zone);
            if (now.isBefore(start)) {
                throw new Exception("Offer not started yet");
            }
        }

        if (offer.getEndDate() != null) {
            ZonedDateTime end = offer.getEndDate().atZone(zone);
            if (now.isAfter(end)) {
                throw new Exception("Offer expired");
            }
        }

        if (offer.getPartnerId() != null && !offer.getPartnerId().equals(orderDto.getPartnerId())) {
            throw new Exception("Offer not valid for this partner");
        }

        if (offer.getRestaurantId() != null && !offer.getRestaurantId().equals(orderDto.getRestaurantId())) {
            throw new Exception("Offer not valid for this restaurant");
        }

        int maximumRedemptionLimit;

        if (offer.getMaximumRedemptionLimit() == null) {
            throw new Exception("Maximum redemption limit is not configured.");
        }

        try {
            maximumRedemptionLimit = Integer.parseInt(offer.getMaximumRedemptionLimit());
        } catch (NumberFormatException e) {
            throw new Exception("Invalid maximum redemption limit value.");
        }

        if (maximumRedemptionLimit <= 0) {
            throw new Exception("This coupon is not available.");
        }

        if (paidOrderCount >= maximumRedemptionLimit) {
            throw new Exception(
                    "Offer usage limit exceeded. Maximum " + maximumRedemptionLimit + " paid redemptions allowed.");
        }

        // Discount Calculation
        double discountValue = offer.getDiscountValue();
        double appliedOfferAmount;

        if ("PERCENTAGE".equalsIgnoreCase(offer.getOfferType().toString())) {
            appliedOfferAmount = (itemTotalAmount * discountValue) / 100d;
        } else {
            appliedOfferAmount = discountValue;
        }

        if (appliedOfferAmount > itemTotalAmount) {
            appliedOfferAmount = itemTotalAmount;
        }

        // persist usage after all checks pass
        saveOfferUsage(orderDto.getOfferCode(), orderDto.getCustomerId(), orderDto.getPartnerId(), offerUsageCount + 1);

        return appliedOfferAmount;
    }

    private void saveOfferUsage(String offerCode, String customerId, String partnerId, Integer usedCount) {
        OfferUsage existingUsage = offerUsageService.getCustomerIdAndOfferCode(customerId, offerCode);

        if (existingUsage != null) {
            existingUsage.setUsageCount(usedCount);
            offerUsageService.save(existingUsage);
        } else {
            OfferUsage offerUsage = new OfferUsage();
            offerUsage.setOfferCode(offerCode);
            offerUsage.setCustomerId(customerId);
            offerUsage.setPartnerId(partnerId);
            offerUsage.setUsageCount(usedCount);
            offerUsageService.save(offerUsage);
        }
    }

    public PreOrder getPreOrder(OrderDto orderDto, Restaurant restaurant, boolean isPreOrder)
            throws ValidationException {

        PreOrder preOrderConfig = restaurant.getPreOrderConfig();

        if (isPreOrder) {
            if (orderDto.getPreOrderDateTime() == null) {
                throw new ValidationException("Pre-order date time is mandatory.");
            }
            if (preOrderConfig == null) {
                throw new ValidationException("Pre-order is not configured for the restaurant.");
            }
            if (!preOrderConfig.isPreOrderEnabled()) {
                throw new ValidationException("Pre-order is not enabled for the restaurant.");
            }
        }
        return preOrderConfig;
    }

    public void validatePreOrderTime(
            LocalDateTime preOrderDateTime, PreOrder preOrderConfig, List<Restaurant.DeliveryHours> deliveryHours)
            throws ValidationException {

        ZoneId istZone = ZoneId.of("Asia/Kolkata");
        ZoneId utcZone = ZoneOffset.UTC;

        ZonedDateTime preOrderIST = preOrderDateTime.atZone(istZone);
        ZonedDateTime preOrderUTC = preOrderIST.withZoneSameInstant(utcZone);

        ZonedDateTime nowUTC = ZonedDateTime.now(utcZone);

        if (!preOrderUTC.isAfter(nowUTC)) {
            throw new ValidationException("Preorder time must be in the future.");
        }

        String unit = Optional.ofNullable(preOrderConfig.getTimeUnit()).orElse("minutes");

        Duration minLead = unit.equalsIgnoreCase("minutes")
                ? Duration.ofMinutes(preOrderConfig.getMinDuration())
                : Duration.ofHours(preOrderConfig.getMinDuration());

        Duration maxLead = unit.equalsIgnoreCase("minutes")
                ? Duration.ofMinutes(preOrderConfig.getMaxDuration())
                : Duration.ofHours(preOrderConfig.getMaxDuration());

        ZonedDateTime minAllowedUTC = nowUTC.plus(minLead);
        ZonedDateTime maxAllowedUTC = nowUTC.plus(maxLead);

        if (preOrderUTC.isBefore(minAllowedUTC)) {
            throw new ValidationException("Preorder time must be at least " + preOrderConfig.getMinDuration() + " "
                    + unit + " ahead of current time.");
        }

        if (preOrderUTC.isAfter(maxAllowedUTC)) {
            throw new ValidationException(
                    "Preorder time cannot be more than " + preOrderConfig.getMaxDuration() + " " + unit + " from now.");
        }

        LocalTime preOrderTimeIST = preOrderIST.toLocalTime();
        if (!isWithinDeliveryHours(preOrderTimeIST, deliveryHours)) {
            throw new ValidationException("Preorder time must be within restaurant delivery hours.");
        }

        ZonedDateTime fulfillmentIST = preOrderIST.minusHours(1);
        LocalTime fulfillmentTimeIST = fulfillmentIST.toLocalTime();

        if (!isWithinDeliveryHours(fulfillmentTimeIST, deliveryHours)) {
            throw new ValidationException("Restaurant cannot prepare this preorder. Fulfillment time ("
                    + fulfillmentTimeIST + " IST) is outside delivery hours.");
        }
    }

    private <T> CompletableFuture<T> timed(String label, Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(
                () -> {
                    long start = System.currentTimeMillis();
                    T result = supplier.get();
                    long elapsed = System.currentTimeMillis() - start;
                    if (elapsed > 100) {
                        log.warn("Phase1 SLOW [{}] took {}ms", label, elapsed);
                    } else {
                        log.info("Phase1 [{}] took {}ms", label, elapsed);
                    }
                    return result;
                },
                DB_FETCH_POOL);
    }
}
