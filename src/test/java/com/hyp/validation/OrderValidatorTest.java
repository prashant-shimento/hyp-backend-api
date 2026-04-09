package com.hyp.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.hyp.dto.OrderDto;
import com.hyp.entity.*;
import com.hyp.enums.OfferType;
import com.hyp.enums.PartnerType;
import com.hyp.exception.ValidationException;
import com.hyp.model.DeliveryQuote;
import com.hyp.repository.DeliveryQuoteRepository;
import com.hyp.repository.OrderRepository;
import com.hyp.service.*;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderValidatorTest {

    @InjectMocks
    private OrderValidator validator;

    @Mock
    private RestaurantService restaurantService;

    @Mock
    private CustomerService customerService;

    @Mock
    private PartnerService partnerService;

    @Mock
    private ItemService itemService;

    @Mock
    private AddonItemService addonItemService;

    @Mock
    private VariationService variationService;

    @Mock
    private TaxService taxService;

    @Mock
    private AddressService addressService;

    @Mock
    private LocationService locationService;

    @Mock
    private OfferService offerService;

    @Mock
    private OfferUsageService offerUsageService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ReferralTokenService referralTokenService;

    @Mock
    private DeliveryQuoteRepository deliveryQuoteRepository;

    @Mock
    private CacheService cacheService;

    // ══════════════════════════════════════════════════════════════════════
    // SECTION 1 – validatePricesAndCalculate (pure logic, no service mocks)
    // Calls the public method directly with Maps — no DB interaction needed.
    // ══════════════════════════════════════════════════════════════════════

    // ── Single item: correct price + tax ──────────────────────────────────

    @Test
    void singleItem_correctPrice_taxCalculatedOnFullAmount() throws ValidationException {
        OrderDto dto = new OrderDto();
        OrderDto.OrderItem oi = orderItem("i1", 100.0, 1, List.of(itemTax("t1", 5.0)));
        dto.setOrderItems(List.of(oi));
        dto.setTaxAmount(5.0);

        Map<String, Item> itemMap = Map.of("i1", dbItem("i1", 100.0));
        Map<String, Tax> taxMap = Map.of("t1", dbTax("t1", "GST", 5.0));

        OrderValidator.PriceCalculation result = validator.validatePricesAndCalculate(
                dto, itemMap, emptyVariations(), emptyAddons(), taxMap, new ArrayList<>(), 0, null);

        assertThat(result.itemTotal()).isEqualTo(100.0);
        assertThat(result.taxTotal()).isEqualTo(5.0); // 5% of 100
        assertThat(result.itemDiscount()).isEqualTo(0.0);
        assertThat(result.restaurantDiscount()).isEqualTo(0.0);
        assertThat(result.offerAmount()).isEqualTo(0.0);
        // orderTax should be set on the DTO with the accumulated tax
        assertThat(dto.getOrderTax()).hasSize(1);
        assertThat(dto.getOrderTax().get(0).getTax()).isEqualTo(5.0);
    }

    @Test
    void singleItem_priceMismatch_warningEmittedAndPriceCorrected() throws ValidationException {
        OrderDto dto = new OrderDto();
        OrderDto.OrderItem oi = orderItem("i1", 80.0, 1, List.of(itemTax("t1", 5.0))); // client says 80
        dto.setOrderItems(List.of(oi));
        dto.setTaxAmount(5.0);

        Map<String, Item> itemMap = Map.of("i1", dbItem("i1", 100.0)); // server says 100
        Map<String, Tax> taxMap = Map.of("t1", dbTax("t1", "GST", 5.0));

        List<OrderValidator.PriceWarning> warnings = new ArrayList<>();
        validator.validatePricesAndCalculate(dto, itemMap, emptyVariations(), emptyAddons(), taxMap, warnings, 0, null);

        assertThat(warnings)
                .anyMatch(w -> w.type().equals("ITEM_PRICE_MISMATCH")
                        && w.message().contains("client=80.0")
                        && w.message().contains("server=100.0"));
        assertThat(oi.getPrice()).isEqualTo(100.0); // price corrected to server value
    }

    // ── Single item with addon ─────────────────────────────────────────────

    @Test
    void singleItem_withAddon_lineTotalIncludesAddon() throws ValidationException {
        OrderDto.OrderAddonItem addonOi = addonItem("a1", 30.0, 1);
        OrderDto.OrderItem oi = orderItem("i1", 80.0, 1, List.of(itemTax("t1", 8.8)));
        oi.setOrderAddonItems(List.of(addonOi));
        OrderDto dto = new OrderDto();
        dto.setOrderItems(List.of(oi));
        dto.setTaxAmount(8.8);

        // item 80 + addon 30 = 110 line total; 5% tax on 110 = 5.5
        Map<String, Tax> taxMap = Map.of("t1", dbTax("t1", "GST", 5.0));

        List<OrderValidator.PriceWarning> warnings = new ArrayList<>();
        OrderValidator.PriceCalculation result = validator.validatePricesAndCalculate(
                dto,
                Map.of("i1", dbItem("i1", 80.0)),
                emptyVariations(),
                Map.of("a1", dbAddon("a1", 30.0)),
                taxMap,
                warnings,
                0,
                null);

        assertThat(result.itemTotal()).isEqualTo(110.0); // 80 + 30
        assertThat(result.taxTotal()).isEqualTo(5.5); // 5% of 110
        // tax mismatch warning since client sent 8.8 but server computes 5.5
        assertThat(warnings).anyMatch(w -> w.type().equals("ITEM_TAX_MISMATCH"));
    }

    @Test
    void singleItem_addonPriceMismatch_warningEmitted() throws ValidationException {
        OrderDto.OrderAddonItem addonOi = addonItem("a1", 20.0, 1); // client says 20
        OrderDto.OrderItem oi = orderItem("i1", 100.0, 1, List.of(itemTax("t1", 5.0)));
        oi.setOrderAddonItems(List.of(addonOi));
        OrderDto dto = new OrderDto();
        dto.setOrderItems(List.of(oi));
        dto.setTaxAmount(5.0);

        Map<String, Tax> taxMap = Map.of("t1", dbTax("t1", "GST", 5.0));

        List<OrderValidator.PriceWarning> warnings = new ArrayList<>();
        validator.validatePricesAndCalculate(
                dto,
                Map.of("i1", dbItem("i1", 100.0)),
                emptyVariations(),
                Map.of("a1", dbAddon("a1", 35.0)),
                taxMap,
                warnings,
                0,
                null); // server says 35

        assertThat(warnings)
                .anyMatch(w -> w.type().equals("ADDON_PRICE_MISMATCH")
                        && w.message().contains("client=20.0")
                        && w.message().contains("server=35.0"));
        assertThat(addonOi.getPrice()).isEqualTo(35.0);
    }

    // ── Variation item ────────────────────────────────────────────────────

    @Test
    void variationItem_usesVariationPrice_noItemLevelDiscount() throws ValidationException {
        // For a variation: oi.variationId != null, oi.id = variation entity id
        OrderDto.OrderItem oi = orderItem("v1", 90.0, 2, List.of(itemTax("t1", 9.0)));
        oi.setVariationId("vg1"); // marks this as variation
        OrderDto dto = new OrderDto();
        dto.setOrderItems(List.of(oi));
        dto.setTaxAmount(9.0);

        // Variation price = 100 per unit, so lineTotal = 200
        // 5% tax on 200 = 10
        Variation variation = new Variation();
        variation.setId("v1");
        variation.setPrice("100.00");

        Map<String, Tax> taxMap = Map.of("t1", dbTax("t1", "GST", 5.0));
        List<OrderValidator.PriceWarning> warnings = new ArrayList<>();
        OrderValidator.PriceCalculation result = validator.validatePricesAndCalculate(
                dto, emptyItems(), Map.of("v1", variation), emptyAddons(), taxMap, warnings, 0, null);

        assertThat(result.itemTotal()).isEqualTo(200.0); // 100 × 2
        assertThat(result.taxTotal()).isEqualTo(10.0); // 5% of 200
        assertThat(result.itemDiscount()).isEqualTo(0.0); // no item-level discount on variations
        // price mismatch warning since client sent 90 but variation says 100
        assertThat(warnings).anyMatch(w -> w.type().equals("ITEM_PRICE_MISMATCH"));
    }

    @Test
    void variationItem_withAddon_addedToVariationLineTotal() throws ValidationException {
        OrderDto.OrderAddonItem addonOi = addonItem("a1", 20.0, 1);
        OrderDto.OrderItem oi = orderItem("v1", 100.0, 1, List.of(itemTax("t1", 9.0)));
        oi.setVariationId("vg1");
        oi.setOrderAddonItems(List.of(addonOi));
        OrderDto dto = new OrderDto();
        dto.setOrderItems(List.of(oi));
        dto.setTaxAmount(9.0);

        Variation variation = new Variation();
        variation.setId("v1");
        variation.setPrice("100.00");

        Map<String, Tax> taxMap = Map.of("t1", dbTax("t1", "GST", 5.0));

        OrderValidator.PriceCalculation result = validator.validatePricesAndCalculate(
                dto,
                emptyItems(),
                Map.of("v1", variation),
                Map.of("a1", dbAddon("a1", 20.0)),
                taxMap,
                new ArrayList<>(),
                0,
                null);

        assertThat(result.itemTotal()).isEqualTo(120.0); // 100 + 20
    }

    // ── Multiple items ────────────────────────────────────────────────────

    @Test
    void multipleItems_eachPricedAndTaxedIndependently() throws ValidationException {
        // item1: 100 × 2 = 200, 5% tax = 10
        // item2:  60 × 3 = 180, 5% tax = 9
        OrderDto.OrderItem oi1 = orderItem("i1", 100.0, 2, List.of(itemTax("t1", 10.0)));
        OrderDto.OrderItem oi2 = orderItem("i2", 60.0, 3, List.of(itemTax("t1", 9.0)));
        OrderDto dto = new OrderDto();
        dto.setOrderItems(List.of(oi1, oi2));
        dto.setTaxAmount(19.0);

        Map<String, Item> itemMap = Map.of(
                "i1", dbItem("i1", 100.0),
                "i2", dbItem("i2", 60.0));
        Map<String, Tax> taxMap = Map.of("t1", dbTax("t1", "GST", 5.0));

        OrderValidator.PriceCalculation result = validator.validatePricesAndCalculate(
                dto, itemMap, emptyVariations(), emptyAddons(), taxMap, new ArrayList<>(), 0, null);

        assertThat(result.itemTotal()).isEqualTo(380.0); // 200 + 180
        assertThat(result.taxTotal()).isEqualTo(19.0); // 10 + 9
    }

    @Test
    void multipleItems_orderTaxAccumulatedAcrossItems() throws ValidationException {
        // Two items sharing the same tax ID — amounts should be summed in orderTax
        OrderDto.OrderItem oi1 = orderItem("i1", 100.0, 1, List.of(itemTax("t1", 5.0)));
        OrderDto.OrderItem oi2 = orderItem("i2", 200.0, 1, List.of(itemTax("t1", 10.0)));
        OrderDto dto = new OrderDto();
        dto.setOrderItems(List.of(oi1, oi2));
        dto.setTaxAmount(15.0);

        Map<String, Tax> taxMap = Map.of("t1", dbTax("t1", "GST", 5.0));
        validator.validatePricesAndCalculate(
                dto,
                Map.of("i1", dbItem("i1", 100.0), "i2", dbItem("i2", 200.0)),
                emptyVariations(),
                emptyAddons(),
                taxMap,
                new ArrayList<>(),
                0,
                null);

        // orderTax should have one entry with accumulated amount 5 + 10 = 15
        assertThat(dto.getOrderTax()).hasSize(1);
        assertThat(dto.getOrderTax().get(0).getTax()).isEqualTo(15.0);
    }

    // ══════════════════════════════════════════════════════════════════════
    // SECTION 2 – Item-level discount (Level 1)
    // ══════════════════════════════════════════════════════════════════════

    @Test
    void itemDiscount_PERCENTAGE_reducesLineTotalBeforeTax() throws ValidationException {
        // price=100, qty=1, PERCENTAGE offer=20% → discount=20, taxableBase=80, tax(5%)=4
        OrderDto.OrderItem oi = orderItem("i1", 100.0, 1, List.of(itemTax("t1", 4.0)));
        OrderDto dto = new OrderDto();
        dto.setOrderItems(List.of(oi));
        dto.setTaxAmount(4.0);

        Item item = dbItemWithOffer("i1", 100.0, OfferType.PERCENTAGE, 20.0);
        Map<String, Tax> taxMap = Map.of("t1", dbTax("t1", "GST", 5.0));

        OrderValidator.PriceCalculation result = validator.validatePricesAndCalculate(
                dto, Map.of("i1", item), emptyVariations(), emptyAddons(), taxMap, new ArrayList<>(), 0, null);

        assertThat(result.itemDiscount()).isEqualTo(20.0);
        assertThat(result.taxTotal()).isEqualTo(4.0); // 5% of (100 - 20)
    }

    @Test
    void itemDiscount_FLAT_fixedAmountDeducted() throws ValidationException {
        // price=100, qty=1, FLAT offer=15 → discount=15, taxableBase=85, tax(5%)=4.25
        OrderDto.OrderItem oi = orderItem("i1", 100.0, 1, List.of(itemTax("t1", 4.25)));
        OrderDto dto = new OrderDto();
        dto.setOrderItems(List.of(oi));
        dto.setTaxAmount(4.25);

        Item item = dbItemWithOffer("i1", 100.0, OfferType.FLAT, 15.0);
        Map<String, Tax> taxMap = Map.of("t1", dbTax("t1", "GST", 5.0));

        OrderValidator.PriceCalculation result = validator.validatePricesAndCalculate(
                dto, Map.of("i1", item), emptyVariations(), emptyAddons(), taxMap, new ArrayList<>(), 0, null);

        assertThat(result.itemDiscount()).isEqualTo(15.0);
        assertThat(result.taxTotal()).isEqualTo(4.25); // 5% of 85
    }

    @Test
    void itemDiscount_FIXED_PRICE_discountIsLineTotalMinusFixedTotal() throws ValidationException {
        // price=100, qty=2 → lineTotal=200; FIXED_PRICE offerValue=75 → fixedTotal=150; discount=50
        // taxableBase = 200 - 50 = 150, tax(5%) = 7.5
        OrderDto.OrderItem oi = orderItem("i1", 100.0, 2, List.of(itemTax("t1", 7.5)));
        OrderDto dto = new OrderDto();
        dto.setOrderItems(List.of(oi));
        dto.setTaxAmount(7.5);

        Item item = dbItemWithOffer("i1", 100.0, OfferType.FIXED_PRICE, 75.0);
        Map<String, Tax> taxMap = Map.of("t1", dbTax("t1", "GST", 5.0));

        OrderValidator.PriceCalculation result = validator.validatePricesAndCalculate(
                dto, Map.of("i1", item), emptyVariations(), emptyAddons(), taxMap, new ArrayList<>(), 0, null);

        assertThat(result.itemDiscount()).isEqualTo(50.0);
        assertThat(result.taxTotal()).isEqualTo(7.5);
    }

    @Test
    void itemDiscount_notApplied_whenOfferDisabled() throws ValidationException {
        OrderDto.OrderItem oi = orderItem("i1", 100.0, 1, List.of(itemTax("t1", 7.5)));
        OrderDto dto = new OrderDto();
        dto.setOrderItems(List.of(oi));
        dto.setTaxAmount(7.5);

        Item item = dbItem("i1", 100.0);
        item.setOfferEnabled(false); // explicitly disabled
        item.setOfferType(OfferType.PERCENTAGE);
        item.setOfferValue(50.0);
        Map<String, Tax> taxMap = Map.of("t1", dbTax("t1", "GST", 5.0));

        OrderValidator.PriceCalculation result = validator.validatePricesAndCalculate(
                dto, Map.of("i1", item), emptyVariations(), emptyAddons(), taxMap, new ArrayList<>(), 0, null);

        assertThat(result.itemDiscount()).isEqualTo(0.0);
    }

    @Test
    void itemDiscount_mismatch_warningEmittedAndValueCorrected() throws ValidationException {
        // Client says discount=5, server computes 20 (PERCENTAGE 20%)
        OrderDto.OrderItem oi = orderItem("i1", 100.0, 1, List.of(itemTax("t1", 5.0)));
        oi.setItemDiscount(5.0); // wrong client value
        OrderDto dto = new OrderDto();
        dto.setOrderItems(List.of(oi));
        dto.setTaxAmount(5.0);

        Item item = dbItemWithOffer("i1", 100.0, OfferType.PERCENTAGE, 20.0);
        Map<String, Tax> taxMap = Map.of("t1", dbTax("t1", "GST", 5.0));
        List<OrderValidator.PriceWarning> warnings = new ArrayList<>();
        validator.validatePricesAndCalculate(
                dto, Map.of("i1", item), emptyVariations(), emptyAddons(), taxMap, warnings, 0, null);

        assertThat(warnings)
                .anyMatch(w -> w.type().equals("ITEM_DISCOUNT_MISMATCH")
                        && w.message().contains("client=5.0")
                        && w.message().contains("server=20.0"));
        assertThat(oi.getItemDiscount()).isEqualTo(20.0);
    }

    // ══════════════════════════════════════════════════════════════════════
    // SECTION 3 – Restaurant discount (Level 2)
    // ══════════════════════════════════════════════════════════════════════

    @Test
    void restaurantDiscount_appliedToAllItemsAfterItemLevelDiscount() throws ValidationException {
        // item: 100, no item offer → postItemBase = 100
        // restaurant 10% → restDiscount = 10, postRestBase = 90
        // tax 5% on 90 = 4.5
        OrderDto.OrderItem oi = orderItem("i1", 100.0, 1, List.of(itemTax("t1", 4.5)));
        OrderDto dto = new OrderDto();
        dto.setOrderItems(List.of(oi));
        dto.setTaxAmount(4.5);

        Map<String, Tax> taxMap = Map.of("t1", dbTax("t1", "GST", 5.0));

        OrderValidator.PriceCalculation result = validator.validatePricesAndCalculate(
                dto,
                Map.of("i1", dbItem("i1", 100.0)),
                emptyVariations(),
                emptyAddons(),
                taxMap,
                new ArrayList<>(),
                10.0,
                null);

        assertThat(result.restaurantDiscount()).isEqualTo(10.0);
        assertThat(result.taxTotal()).isEqualTo(4.5); // 5% of 90
    }

    @Test
    void restaurantDiscount_appliedEvenToItemsWithItemLevelDiscount() throws ValidationException {
        // item: price=100, FLAT item discount=20 → postItemBase=80
        // restaurant 10% on 80 → restDiscount=8, postRestBase=72
        // tax 5% on 72 = 3.6
        OrderDto.OrderItem oi = orderItem("i1", 100.0, 1, List.of(itemTax("t1", 3.6)));
        OrderDto dto = new OrderDto();
        dto.setOrderItems(List.of(oi));
        dto.setTaxAmount(3.6);

        Item item = dbItemWithOffer("i1", 100.0, OfferType.FLAT, 20.0);
        Map<String, Tax> taxMap = Map.of("t1", dbTax("t1", "GST", 5.0));

        OrderValidator.PriceCalculation result = validator.validatePricesAndCalculate(
                dto, Map.of("i1", item), emptyVariations(), emptyAddons(), taxMap, new ArrayList<>(), 10.0, null);

        assertThat(result.itemDiscount()).isEqualTo(20.0);
        assertThat(result.restaurantDiscount()).isEqualTo(8.0);
        assertThat(result.taxTotal()).isEqualTo(3.6);
    }

    @Test
    void restaurantDiscount_multipleItems_summedCorrectly() throws ValidationException {
        // item1: 100 → restDiscount=10; item2: 200 → restDiscount=20; total restDiscount=30
        OrderDto.OrderItem oi1 = orderItem("i1", 100.0, 1, List.of(itemTax("t1", 3.6)));
        OrderDto.OrderItem oi2 = orderItem("i2", 200.0, 1, List.of(itemTax("t1", 3.6)));
        OrderDto dto = new OrderDto();
        dto.setOrderItems(List.of(oi1, oi2));
        dto.setTaxAmount(3.6);
        Map<String, Tax> taxMap = Map.of("t1", dbTax("t1", "GST", 5.0));

        OrderValidator.PriceCalculation result = validator.validatePricesAndCalculate(
                dto,
                Map.of("i1", dbItem("i1", 100.0), "i2", dbItem("i2", 200.0)),
                emptyVariations(),
                emptyAddons(),
                taxMap,
                new ArrayList<>(),
                10.0,
                null);

        assertThat(result.restaurantDiscount()).isEqualTo(30.0);
    }

    // ══════════════════════════════════════════════════════════════════════
    // SECTION 4 – Customer offer (Level 3)
    // ══════════════════════════════════════════════════════════════════════

    @Test
    void customerOffer_PERCENTAGE_sameRateAppliedPerItem() throws ValidationException {
        // postRestBase = 90 (after 10% restaurant on 100); offer PERCENTAGE 10%
        // offerForItem = 90 * 10% = 9; taxableBase = 81; tax 5% = 4.05
        OrderDto.OrderItem oi = orderItem("i1", 100.0, 1, List.of(itemTax("t1", 4.05)));
        OrderDto dto = new OrderDto();
        dto.setOrderItems(List.of(oi));
        dto.setTaxAmount(4.05);

        Offer offer = offerWithPercentage(10.0);
        Map<String, Tax> taxMap = Map.of("t1", dbTax("t1", "GST", 5.0));

        OrderValidator.PriceCalculation result = validator.validatePricesAndCalculate(
                dto,
                Map.of("i1", dbItem("i1", 100.0)),
                emptyVariations(),
                emptyAddons(),
                taxMap,
                new ArrayList<>(),
                10.0,
                offer);

        assertThat(result.offerAmount()).isEqualTo(9.0);
        assertThat(result.taxTotal()).isEqualTo(4.05); // 5% of 81
    }

    @Test
    void customerOffer_FLAT_distributedProportionallyAcrossItems() throws ValidationException {
        // item1: postRestBase=100, item2: postRestBase=60, total=160
        // FLAT offer=40; item1 share = 40*(100/160)=25, item2 share = 40*(60/160)=15
        // item1 taxableBase=75, tax 5%=3.75; item2 taxableBase=45, tax 5%=2.25 → total tax=6
        OrderDto.OrderItem oi1 = orderItem("i1", 100.0, 1, List.of(itemTax("t1", 3.75)));
        OrderDto.OrderItem oi2 = orderItem("i2", 60.0, 1, List.of(itemTax("t1", 2.25)));
        OrderDto dto = new OrderDto();
        dto.setOrderItems(List.of(oi1, oi2));
        dto.setTaxAmount(6.0);

        Offer offer = offerFlat(40.0);
        Map<String, Tax> taxMap = Map.of("t1", dbTax("t1", "GST", 5.0));

        OrderValidator.PriceCalculation result = validator.validatePricesAndCalculate(
                dto,
                Map.of("i1", dbItem("i1", 100.0), "i2", dbItem("i2", 60.0)),
                emptyVariations(),
                emptyAddons(),
                taxMap,
                new ArrayList<>(),
                0,
                offer);

        assertThat(result.offerAmount()).isEqualTo(40.0);
        assertThat(result.taxTotal()).isEqualTo(6.0);
    }

    @Test
    void customerOffer_FLAT_cappedAtTotalPostRestBase() throws ValidationException {
        // item: postRestBase=50; FLAT offer=100 (exceeds total) → capped at 50
        OrderDto.OrderItem oi = orderItem("i1", 50.0, 1, List.of(itemTax("t1", 0.0)));
        OrderDto dto = new OrderDto();
        dto.setOrderItems(List.of(oi));
        dto.setTaxAmount(0.0);

        Offer offer = offerFlat(100.0);
        Map<String, Tax> taxMap = Map.of("t1", dbTax("t1", "GST", 5.0));

        OrderValidator.PriceCalculation result = validator.validatePricesAndCalculate(
                dto,
                Map.of("i1", dbItem("i1", 50.0)),
                emptyVariations(),
                emptyAddons(),
                taxMap,
                new ArrayList<>(),
                0,
                offer);

        assertThat(result.offerAmount()).isEqualTo(50.0); // capped at postRestBase total
        assertThat(result.taxTotal()).isEqualTo(0.0); // taxableBase = 0, no tax
    }

    @Test
    void allThreeDiscountLevels_taxComputedOnFullyDiscountedBase() throws ValidationException {
        // price=120, qty=1
        // Level1 PERCENTAGE 10%: itemDiscount=12, postItemBase=108
        // Level2 restaurant 5%: restDiscount=5.4, postRestBase=102.6
        // Level3 offer PERCENTAGE 10%: offerAmount=10.26, taxableBase=92.34
        // tax 5% on 92.34 = 4.617 → rounded = 4.62
        OrderDto.OrderItem oi = orderItem("i1", 120.0, 1, List.of(itemTax("t1", 4.62)));
        OrderDto dto = new OrderDto();
        dto.setOrderItems(List.of(oi));
        dto.setTaxAmount(4.62);

        Item item = dbItemWithOffer("i1", 120.0, OfferType.PERCENTAGE, 10.0);
        Offer offer = offerWithPercentage(10.0);
        Map<String, Tax> taxMap = Map.of("t1", dbTax("t1", "GST", 5.0));

        OrderValidator.PriceCalculation result = validator.validatePricesAndCalculate(
                dto, Map.of("i1", item), emptyVariations(), emptyAddons(), taxMap, new ArrayList<>(), 5.0, offer);

        assertThat(result.itemDiscount()).isEqualTo(12.0);
        assertThat(result.restaurantDiscount()).isEqualTo(5.4);
        assertThat(result.offerAmount()).isEqualTo(10.26);
        assertThat(result.taxTotal()).isEqualTo(4.62);
    }

    // ── Tax mismatch warning ───────────────────────────────────────────────

    @Test
    void taxMismatch_warningEmittedAndAmountCorrected() throws ValidationException {
        // client sends tax=99 but server computes 5% of 100 = 5
        OrderDto.OrderItem oi = orderItem("i1", 100.0, 1, List.of(itemTax("t1", 99.0)));
        OrderDto dto = new OrderDto();
        dto.setOrderItems(List.of(oi));
        dto.setTaxAmount(5.0); // correct total

        Map<String, Tax> taxMap = Map.of("t1", dbTax("t1", "GST", 5.0));

        List<OrderValidator.PriceWarning> warnings = new ArrayList<>();
        validator.validatePricesAndCalculate(
                dto, Map.of("i1", dbItem("i1", 100.0)), emptyVariations(), emptyAddons(), taxMap, warnings, 0, null);

        assertThat(warnings)
                .anyMatch(w -> w.type().equals("ITEM_TAX_MISMATCH")
                        && w.message().contains("client=99.0")
                        && w.message().contains("server=5.0"));
        assertThat(oi.getOrderItemTax().get(0).getAmount()).isEqualTo(5.0);
    }

    // ══════════════════════════════════════════════════════════════════════
    // SECTION 5 – validate() full flow (service mocks)
    // ══════════════════════════════════════════════════════════════════════

    // ── Entity validation ─────────────────────────────────────────────────

    @Test
    void validate_restaurantNotServiceable_throwsValidationException() {
        OrderDto dto = dineinOrderDto("rest1", "cust1", singleItemList("i1", 100.0, 1));
        Restaurant restaurant = basicRestaurant("rest1");
        restaurant.setServiceable(false);

        stubBasicServices(dto, restaurant, List.of(dbItem("i1", 100.0)), List.of());

        assertThatThrownBy(() -> validator.validate(dto))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("not serviceable");
    }

    @Test
    void validate_restaurantNotActive_throwsValidationException() {
        OrderDto dto = dineinOrderDto("rest1", "cust1", singleItemList("i1", 100.0, 1));
        Restaurant restaurant = basicRestaurant("rest1");
        restaurant.setActive(false);

        stubBasicServices(dto, restaurant, List.of(dbItem("i1", 100.0)), List.of());

        assertThatThrownBy(() -> validator.validate(dto))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("not active");
    }

    @Test
    void validate_customerNotFound_throwsEntityNotFoundException() {
        OrderDto dto = dineinOrderDto("rest1", "cust1", singleItemList("i1", 100.0, 1));
        Restaurant restaurant = basicRestaurant("rest1");

        stubBasicServices(dto, restaurant, List.of(dbItem("i1", 100.0)), List.of());
        when(customerService.findById("cust1")).thenReturn(null); // override to null

        assertThatThrownBy(() -> validator.validate(dto)).hasMessageContaining("Customer");
    }

    // ── Packaging charge ──────────────────────────────────────────────────

    @Test
    void validate_packagingCharge_computedAsPerItemRateTimesQuantity() throws Exception {
        // 2 items: qty=2 and qty=3; restaurant packagingCharge=10 per item
        // serverPackagingCharge = 10×2 + 10×3 = 50
        List<OrderDto.OrderItem> items = List.of(orderItem("i1", 100.0, 2, null), orderItem("i2", 50.0, 3, null));
        OrderDto dto = dineinOrderDto("rest1", "cust1", items);
        dto.setPackagingCharge(0.0); // wrong client value

        Restaurant restaurant = basicRestaurant("rest1");
        restaurant.setPackagingCharge("10");

        stubBasicServices(dto, restaurant, List.of(dbItem("i1", 100.0), dbItem("i2", 50.0)), List.of());
        when(cacheService.isServerCorrectionEnabled(anyString())).thenReturn(true);

        validator.validate(dto);

        assertThat(dto.getPackagingCharge()).isEqualTo(50.0);
    }

    @Test
    void validate_packagingCharge_mismatch_warningEmitted() throws Exception {
        List<OrderDto.OrderItem> items = List.of(orderItem("i1", 100.0, 1, null));
        OrderDto dto = dineinOrderDto("rest1", "cust1", items);
        dto.setPackagingCharge(99.0); // wrong client value

        Restaurant restaurant = basicRestaurant("rest1");
        restaurant.setPackagingCharge("10");

        stubBasicServices(dto, restaurant, List.of(dbItem("i1", 100.0)), List.of());
        when(cacheService.isServerCorrectionEnabled(anyString())).thenReturn(false);

        OrderValidator.OrderValidationResult result = validator.validate(dto);

        assertThat(result.priceWarnings())
                .anyMatch(w -> w.type().equals("PACKAGING_CHARGE_MISMATCH")
                        && w.message().contains("client=99.0")
                        && w.message().contains("server=10.0"));
        // correction off — original value NOT overridden
        assertThat(dto.getPackagingCharge()).isEqualTo(99.0);
    }

    @Test
    void validate_pcTax_computedFromRestaurantTaxConfig() throws Exception {
        // packagingCharge=100, pcTax 10% → serverPcTaxAmount=10
        List<OrderDto.OrderItem> items = List.of(orderItem("i1", 100.0, 1, null));
        OrderDto dto = dineinOrderDto("rest1", "cust1", items);
        dto.setPackagingCharge(100.0);
        dto.setPcTaxAmount(0.0);

        Restaurant restaurant = basicRestaurant("rest1");
        restaurant.setPackagingCharge("100");
        restaurant.setCalculateTaxOnPacking(1);
        Restaurant.RestaurantTax rTax = new Restaurant.RestaurantTax();
        rTax.setPcTaxesId("pctax1");
        restaurant.setTax(rTax);

        Tax pcTax = dbTax("pctax1", "PC_TAX", 10.0);

        stubBasicServices(dto, restaurant, List.of(dbItem("i1", 100.0)), List.of());
        when(taxService.findAllByIdIn(anyList())).thenReturn(List.of(pcTax));
        when(cacheService.isServerCorrectionEnabled(anyString())).thenReturn(true);

        validator.validate(dto);

        assertThat(dto.getPcTaxAmount()).isEqualTo(10.0); // 10% of 100
    }

    // ── Delivery charge ───────────────────────────────────────────────────

    @Test
    void validate_deliveryCharge_discountedByTotalDeliveryShare() throws Exception {
        // quotePrice=100, totalDeliveryShare=30% → serverDeliveryCharge = 70
        List<OrderDto.OrderItem> items = List.of(orderItem("i1", 100.0, 1, null));
        OrderDto dto = dineinOrderDto("rest1", "cust1", items);
        dto.getDeliveryDetails().setDeliveryQuoteId("q1");
        dto.setDeliveryCharge(100.0);

        Restaurant restaurant = basicRestaurant("rest1");
        restaurant.setTotalDeliveryShare(30.0);

        stubBasicServices(dto, restaurant, List.of(dbItem("i1", 100.0)), List.of());
        when(deliveryQuoteRepository.findById("q1")).thenReturn(Optional.of(buildQuote("q1", 100.0)));
        when(cacheService.isServerCorrectionEnabled(anyString())).thenReturn(true);

        validator.validate(dto);

        assertThat(dto.getDeliveryCharge()).isEqualTo(70.0);
    }

    @Test
    void validate_deliveryCharge_fallsBackToSumOfRestAndPlatformShares() throws Exception {
        // totalDeliveryShare=null, restaurantShare=15, platformShare=10 → effective 25%
        // quotePrice=80 → serverDeliveryCharge = 80 - 25% = 60
        List<OrderDto.OrderItem> items = List.of(orderItem("i1", 50.0, 1, null));
        OrderDto dto = dineinOrderDto("rest1", "cust1", items);
        dto.getDeliveryDetails().setDeliveryQuoteId("q1");
        dto.setDeliveryCharge(80.0);

        Restaurant restaurant = basicRestaurant("rest1");
        restaurant.setTotalDeliveryShare(null);
        restaurant.setRestaurantDeliveryShare(15.0);
        restaurant.setPlatformDeliveryShare(10.0);

        stubBasicServices(dto, restaurant, List.of(dbItem("i1", 50.0)), List.of());
        when(deliveryQuoteRepository.findById("q1")).thenReturn(Optional.of(buildQuote("q1", 80.0)));
        when(cacheService.isServerCorrectionEnabled(anyString())).thenReturn(true);

        validator.validate(dto);

        assertThat(dto.getDeliveryCharge()).isEqualTo(60.0);
    }

    @Test
    void validate_deliveryCharge_mismatch_warningEmitted() throws Exception {
        // quotePrice=100, client sent 80 → DELIVERY_CHARGE_MISMATCH warning
        List<OrderDto.OrderItem> items = List.of(orderItem("i1", 50.0, 1, null));
        OrderDto dto = dineinOrderDto("rest1", "cust1", items);
        dto.getDeliveryDetails().setDeliveryQuoteId("q1");
        dto.setDeliveryCharge(80.0); // client value

        Restaurant restaurant = basicRestaurant("rest1");

        stubBasicServices(dto, restaurant, List.of(dbItem("i1", 50.0)), List.of());
        when(deliveryQuoteRepository.findById("q1")).thenReturn(Optional.of(buildQuote("q1", 100.0)));
        when(cacheService.isServerCorrectionEnabled(anyString())).thenReturn(false);

        OrderValidator.OrderValidationResult result = validator.validate(dto);

        assertThat(result.priceWarnings()).anyMatch(w -> w.type().equals("DELIVERY_CHARGE_MISMATCH"));
    }

    @Test
    void validate_dcTax_computedWhenFlagEnabled() throws Exception {
        // deliveryCharge=100 after quote, calculateTaxOnDelivery=1, dcTaxRate=5% → dcTaxAmount=5
        List<OrderDto.OrderItem> items = List.of(orderItem("i1", 100.0, 1, null));
        OrderDto dto = dineinOrderDto("rest1", "cust1", items);
        dto.getDeliveryDetails().setDeliveryQuoteId("q1");
        dto.setDeliveryCharge(100.0);
        dto.setDcTaxAmount(0.0);

        Restaurant restaurant = basicRestaurant("rest1");
        restaurant.setCalculateTaxOnDelivery(1);
        Restaurant.RestaurantTax rTax = new Restaurant.RestaurantTax();
        rTax.setDcTaxesId("dctax1");
        restaurant.setTax(rTax);

        Tax dcTax = dbTax("dctax1", "DC_TAX", 5.0);

        stubBasicServices(dto, restaurant, List.of(dbItem("i1", 100.0)), List.of());
        when(taxService.findAllByIdIn(anyList())).thenReturn(List.of(dcTax));
        when(deliveryQuoteRepository.findById("q1")).thenReturn(Optional.of(buildQuote("q1", 100.0)));
        when(cacheService.isServerCorrectionEnabled(anyString())).thenReturn(true);

        validator.validate(dto);

        assertThat(dto.getDcTaxAmount()).isEqualTo(5.0);
    }

    // ── Server correction flag ────────────────────────────────────────────

    @Test
    void validate_serverCorrectionOff_warningsEmittedButValuesNotOverridden() throws Exception {
        // Send wrong prices on purpose; with correction=false, DTO values stay as sent
        List<OrderDto.OrderItem> items = List.of(orderItem("i1", 50.0, 1, null)); // client says 50, DB says 100
        OrderDto dto = dineinOrderDto("rest1", "cust1", items);
        dto.setTaxAmount(999.0);
        dto.setGrandTotalAmount(999.0);
        dto.setTotalAmount(999.0);
        dto.setDiscountAmount(999.0);

        Restaurant restaurant = basicRestaurant("rest1");
        stubBasicServices(dto, restaurant, List.of(dbItem("i1", 100.0)), List.of()); // DB price 100
        when(cacheService.isServerCorrectionEnabled(anyString())).thenReturn(false);

        OrderValidator.OrderValidationResult result = validator.validate(dto);

        assertThat(result.priceWarnings()).isNotEmpty(); // warnings fired
        assertThat(dto.getGrandTotalAmount()).isEqualTo(999.0); // not overridden
        assertThat(result.serverCorrectionApplied()).isFalse();
    }

    @Test
    void validate_serverCorrectionOn_overridesClientValues() throws Exception {
        // item: 100×1, no tax, no discount, no packaging, no delivery
        // grandTotal should be 100
        List<OrderDto.OrderItem> items = List.of(orderItem("i1", 100.0, 1, null));
        OrderDto dto = dineinOrderDto("rest1", "cust1", items);
        dto.setGrandTotalAmount(999.0); // wrong client value

        Restaurant restaurant = basicRestaurant("rest1");
        stubBasicServices(dto, restaurant, List.of(dbItem("i1", 100.0)), List.of());
        when(cacheService.isServerCorrectionEnabled(anyString())).thenReturn(true);

        validator.validate(dto);

        assertThat(dto.getGrandTotalAmount()).isEqualTo(100.0);
    }

    @Test
    void validate_itemTotalAmount_alwaysSetEvenWhenCorrectionOff() throws Exception {
        List<OrderDto.OrderItem> items = List.of(orderItem("i1", 100.0, 1, null));
        OrderDto dto = dineinOrderDto("rest1", "cust1", items);

        Restaurant restaurant = basicRestaurant("rest1");
        stubBasicServices(dto, restaurant, List.of(dbItem("i1", 100.0)), List.of());
        when(cacheService.isServerCorrectionEnabled(anyString())).thenReturn(false);

        validator.validate(dto);

        // itemTotalAmount is always set regardless of correction flag
        assertThat(dto.getItemTotalAmount()).isEqualTo(100.0);
    }

    // ── Offer validation ──────────────────────────────────────────────────

    @Test
    void validate_offer_expired_throws() {
        List<OrderDto.OrderItem> items = List.of(orderItem("i1", 100.0, 1, null));
        OrderDto dto = dineinOrderDto("rest1", "cust1", items);
        dto.setOfferCode("EXPIRED10");

        Offer offer = activeOffer("EXPIRED10", OfferType.PERCENTAGE, 10.0, "p1");
        offer.setEndDate(LocalDateTime.now().minusDays(1)); // expired yesterday

        stubBasicServices(dto, basicRestaurant("rest1"), List.of(dbItem("i1", 100.0)), List.of());
        when(offerService.findByOfferCode("EXPIRED10")).thenReturn(offer);
        when(orderRepository.countByCustomerIdAndStatus("cust1", "PAID")).thenReturn(0L);
        when(offerUsageService.getUsageCount("EXPIRED10", "cust1")).thenReturn(0);

        assertThatThrownBy(() -> validator.validate(dto)).hasMessageContaining("expired");
    }

    @Test
    void validate_offer_notActive_throws() {
        List<OrderDto.OrderItem> items = List.of(orderItem("i1", 100.0, 1, null));
        OrderDto dto = dineinOrderDto("rest1", "cust1", items);
        dto.setOfferCode("INACTIVE");

        Offer offer = activeOffer("INACTIVE", OfferType.PERCENTAGE, 10.0, "p1");
        offer.setIsActive(false);

        stubBasicServices(dto, basicRestaurant("rest1"), List.of(dbItem("i1", 100.0)), List.of());
        when(offerService.findByOfferCode("INACTIVE")).thenReturn(offer);
        when(orderRepository.countByCustomerIdAndStatus("cust1", "PAID")).thenReturn(0L);
        when(offerUsageService.getUsageCount("INACTIVE", "cust1")).thenReturn(0);

        assertThatThrownBy(() -> validator.validate(dto)).hasMessageContaining("not active");
    }

    @Test
    void validate_offer_usageLimitExceeded_throws() {
        List<OrderDto.OrderItem> items = List.of(orderItem("i1", 100.0, 1, null));
        OrderDto dto = dineinOrderDto("rest1", "cust1", items);
        dto.setOfferCode("ONCE");

        Offer offer = activeOffer("ONCE", OfferType.PERCENTAGE, 10.0, "p1");
        offer.setMaximumRedemptionLimit("1"); // max 1 use

        stubBasicServices(dto, basicRestaurant("rest1"), List.of(dbItem("i1", 100.0)), List.of());
        when(partnerService.findPartnersByRestaurantId(eq(dto.getRestaurantId()), eq(PartnerType.RESTAURANT)))
                .thenReturn(buildPartner("p1"));
        when(offerService.findByOfferCode("ONCE")).thenReturn(offer);
        when(orderRepository.countByCustomerIdAndStatus("cust1", "PAID")).thenReturn(5L); // already 5 paid orders
        when(offerUsageService.getUsageCount("ONCE", "cust1")).thenReturn(1); // already used once

        assertThatThrownBy(() -> validator.validate(dto)).hasMessageContaining("limit exceeded");
    }

    @Test
    void validate_offer_invalidPartner_throws() {
        List<OrderDto.OrderItem> items = List.of(orderItem("i1", 100.0, 1, null));
        OrderDto dto = dineinOrderDto("rest1", "cust1", items);
        dto.setOfferCode("PARTNER_ONLY");

        Offer offer = activeOffer("PARTNER_ONLY", OfferType.PERCENTAGE, 10.0, "partner1");
        offer.setPartnerId("other_partner"); // belongs to a different partner

        stubBasicServices(dto, basicRestaurant("rest1"), List.of(dbItem("i1", 100.0)), List.of());
        when(offerService.findByOfferCode("PARTNER_ONLY")).thenReturn(offer);
        when(orderRepository.countByCustomerIdAndStatus("cust1", "PAID")).thenReturn(0L);
        when(offerUsageService.getUsageCount("PARTNER_ONLY", "cust1")).thenReturn(0);

        assertThatThrownBy(() -> validator.validate(dto)).hasMessageContaining("not valid for this partner");
    }

    @Test
    void validate_offer_PERCENTAGE_appliedAndReflectedInResult() throws Exception {
        // item: 100, no restaurant discount
        // offer PERCENTAGE 10% → offerAmount = 10, grandTotal = 100 - 10 = 90
        List<OrderDto.OrderItem> items = List.of(orderItem("i1", 100.0, 1, null));
        OrderDto dto = dineinOrderDto("rest1", "cust1", items);
        dto.setOfferCode("SAVE10");
        dto.setDiscountAmount(0.0);

        Offer offer = activeOffer("SAVE10", OfferType.PERCENTAGE, 10.0, "partner1");
        offer.setMaximumRedemptionLimit("100");

        stubBasicServices(dto, basicRestaurant("rest1"), List.of(dbItem("i1", 100.0)), List.of());
        when(offerService.findByOfferCode("SAVE10")).thenReturn(offer);
        when(orderRepository.countByCustomerIdAndStatus("cust1", "PAID")).thenReturn(0L);
        when(offerUsageService.getUsageCount("SAVE10", "cust1")).thenReturn(0);
        when(offerUsageService.getCustomerIdAndOfferCode("cust1", "SAVE10")).thenReturn(null);
        when(offerUsageService.save(any())).thenReturn(null);
        when(cacheService.isServerCorrectionEnabled(anyString())).thenReturn(true);

        OrderValidator.OrderValidationResult result = validator.validate(dto);

        assertThat(result.discountAmount()).isEqualTo(10.0);
        assertThat(result.grandTotalAmount()).isEqualTo(90.0);
    }

    @Test
    void validate_offer_FLAT_appliedAndReflectedInResult() throws Exception {
        // item: 100, FLAT offer=25 → grandTotal = 100 - 25 = 75
        List<OrderDto.OrderItem> items = List.of(orderItem("i1", 100.0, 1, null));
        OrderDto dto = dineinOrderDto("rest1", "cust1", items);
        dto.setOfferCode("FLAT25");
        dto.setDiscountAmount(0.0);

        Offer offer = activeOffer("FLAT25", OfferType.FLAT, 25.0, "partner1");
        offer.setMaximumRedemptionLimit("100");

        stubBasicServices(dto, basicRestaurant("rest1"), List.of(dbItem("i1", 100.0)), List.of());
        when(offerService.findByOfferCode("FLAT25")).thenReturn(offer);
        when(orderRepository.countByCustomerIdAndStatus("cust1", "PAID")).thenReturn(0L);
        when(offerUsageService.getUsageCount("FLAT25", "cust1")).thenReturn(0);
        when(offerUsageService.getCustomerIdAndOfferCode("cust1", "FLAT25")).thenReturn(null);
        when(offerUsageService.save(any())).thenReturn(null);
        when(cacheService.isServerCorrectionEnabled(anyString())).thenReturn(true);

        OrderValidator.OrderValidationResult result = validator.validate(dto);

        assertThat(result.discountAmount()).isEqualTo(25.0);
        assertThat(result.grandTotalAmount()).isEqualTo(75.0);
    }

    // ══════════════════════════════════════════════════════════════════════
    // SECTION 6 – Grand total formula
    // ══════════════════════════════════════════════════════════════════════

    @Test
    void validate_grandTotal_sumOfAllComponents() throws Exception {
        // item=100, tax(5%)=5, delivery=20, packaging=10, no discount
        // grandTotal = 100 + 5 + 20 + 10 = 135
        List<OrderDto.OrderItem> items = List.of(orderItem("i1", 100.0, 1, List.of(itemTax("t1", 5.0))));
        OrderDto dto = dineinOrderDto("rest1", "cust1", items);
        dto.getDeliveryDetails().setDeliveryQuoteId("q1");
        dto.setDeliveryCharge(20.0);
        dto.setTaxAmount(5.0);
        dto.setGrandTotalAmount(0.0);

        Restaurant restaurant = basicRestaurant("rest1");
        restaurant.setPackagingCharge("10");

        Tax tax = dbTax("t1", "GST", 5.0);

        stubBasicServices(dto, restaurant, List.of(dbItem("i1", 100.0)), List.of(tax));
        when(deliveryQuoteRepository.findById("q1")).thenReturn(Optional.of(buildQuote("q1", 20.0)));
        when(cacheService.isServerCorrectionEnabled(anyString())).thenReturn(true);

        validator.validate(dto);

        assertThat(dto.getGrandTotalAmount()).isEqualTo(135.0); // 100 + 5 + 20 + 10
    }

    // ══════════════════════════════════════════════════════════════════════
    // Helpers
    // ══════════════════════════════════════════════════════════════════════

    // ── DTO builders ─────────────────────────────────────────────────────

    private OrderDto dineinOrderDto(String restaurantId, String customerId, List<OrderDto.OrderItem> items) {
        OrderDto dto = new OrderDto();
        dto.setRestaurantId(restaurantId);
        dto.setCustomerId(customerId);
        dto.setOrderType("2"); // DineIn — skips address validation
        dto.setPaymentType("CASH");
        dto.setOrderTime("12:00");
        dto.setSpecialInstructions("none");
        dto.setTotalAmount(0.0);
        dto.setGrandTotalAmount(0.0);
        dto.setDiscountAmount(0.0);
        dto.setTaxAmount(0.0);
        dto.setDeliveryCharge(0.0);
        dto.setPackagingCharge(0.0);
        dto.setOrderItems(new ArrayList<>(items));
        // DeliveryDetails needed for deliveryQuoteId; addressId left null → no address fetch
        OrderDto.DeliveryDetails dd = new OrderDto.DeliveryDetails();
        dto.setDeliveryDetails(dd);
        return dto;
    }

    private List<OrderDto.OrderItem> singleItemList(String id, double price, int qty) {
        return List.of(orderItem(id, price, qty, null));
    }

    private OrderDto.OrderItem orderItem(String id, double price, int qty, List<OrderDto.OrderItemTax> taxes) {
        OrderDto.OrderItem oi = new OrderDto.OrderItem();
        oi.setId(id);
        oi.setName(id + "_name");
        oi.setPrice(price);
        oi.setQuantity(qty);
        oi.setFinalPrice(price * qty);
        oi.setItemDiscount(0.0);
        if (taxes != null) oi.setOrderItemTax(new ArrayList<>(taxes));
        return oi;
    }

    private OrderDto.OrderItemTax itemTax(String id, double amount) {
        OrderDto.OrderItemTax t = new OrderDto.OrderItemTax();
        t.setId(id);
        t.setName(id + "_name");
        t.setAmount(amount);
        return t;
    }

    private OrderDto.OrderAddonItem addonItem(String addonItemId, double price, int qty) {
        OrderDto.OrderAddonItem a = new OrderDto.OrderAddonItem();
        a.setAddonItemId(addonItemId);
        a.setAddonItemName(addonItemId + "_name");
        a.setPrice(price);
        a.setQuantity(qty);
        return a;
    }

    // ── Entity builders ───────────────────────────────────────────────────

    private Restaurant basicRestaurant(String id) {
        Restaurant r = new Restaurant();
        r.setId(id);
        r.setActive(true);
        r.setServiceable(true);
        // "00:00"→"00:00": from.equals(to) → always within delivery hours
        r.setDeliveryHours(List.of(new Restaurant.DeliveryHours("00:00", "00:00")));
        r.setPackagingCharge("0");
        return r;
    }

    private Item dbItem(String id, double price) {
        Item item = new Item();
        item.setId(id);
        item.setPrice(String.valueOf(price));
        item.setOfferEnabled(false);
        return item;
    }

    private Item dbItemWithOffer(String id, double price, OfferType type, double value) {
        Item item = dbItem(id, price);
        item.setOfferEnabled(true);
        item.setOfferType(type);
        item.setOfferValue(value);
        return item;
    }

    private Tax dbTax(String id, String name, double rate) {
        Tax tax = new Tax();
        tax.setId(id);
        tax.setTaxName(name);
        tax.setTaxType("GST");
        tax.setTax(String.valueOf(rate));
        return tax;
    }

    private Variation dbVariation(String id, double price) {
        Variation v = new Variation();
        v.setId(id);
        v.setPrice(String.valueOf(price));
        return v;
    }

    private AddonItem dbAddon(String id, double price) {
        AddonItem a = new AddonItem();
        a.setId(id);
        a.setAddonItemPrice(String.valueOf(price));
        return a;
    }

    private Offer activeOffer(String code, OfferType type, double value, String partnerId) {
        Offer offer = new Offer();
        offer.setId("offer_" + code);
        offer.setOfferCode(code);
        offer.setOfferType(type);
        offer.setDiscountValue(value);
        offer.setIsActive(true);
        offer.setPartnerId(partnerId);
        offer.setStartDate(LocalDateTime.now().minusDays(10));
        offer.setEndDate(LocalDateTime.now().plusDays(10));
        offer.setMaximumRedemptionLimit("100");
        return offer;
    }

    private Offer offerWithPercentage(double pct) {
        return activeOffer("PCT" + pct, OfferType.PERCENTAGE, pct, null);
    }

    private Offer offerFlat(double value) {
        return activeOffer("FLAT" + value, OfferType.FLAT, value, null);
    }

    private DeliveryQuoteRecord buildQuote(String id, double price) {
        DeliveryQuoteRecord record = new DeliveryQuoteRecord();
        record.setId(id);
        DeliveryQuote.DeliveryNetworks network = new DeliveryQuote.DeliveryNetworks();
        DeliveryQuote.Quote quote = new DeliveryQuote.Quote();
        quote.setPrice(price);
        network.setQuote(quote);
        record.setNetwork(network);
        return record;
    }

    // ── Empty maps ────────────────────────────────────────────────────────

    private Map<String, Item> emptyItems() {
        return Collections.emptyMap();
    }

    private Map<String, Variation> emptyVariations() {
        return Collections.emptyMap();
    }

    private Map<String, AddonItem> emptyAddons() {
        return Collections.emptyMap();
    }

    // ── Service stubbing ──────────────────────────────────────────────────

    /**
     * Stubs the minimum set of services required for a validate() call on a
     * DineIn order with no offer, no referral token, and no delivery quote.
     */
    private void stubBasicServices(OrderDto dto, Restaurant restaurant, List<Item> items, List<Tax> taxes) {
        when(restaurantService.findById(dto.getRestaurantId())).thenReturn(restaurant);
        when(customerService.findById(dto.getCustomerId())).thenReturn(buildCustomer(dto.getCustomerId()));
        // partnerId is null on the DTO → resolves via findPartnersByRestaurantId
        when(partnerService.findPartnersByRestaurantId(eq(dto.getRestaurantId()), eq(PartnerType.RESTAURANT)))
                .thenReturn(buildPartner("partner1"));
        when(itemService.findAllByIdIn(anyList())).thenReturn(items);
        when(variationService.findAllByIdIn(anyList())).thenReturn(Collections.emptyList());
        when(addonItemService.findAllByIdIn(anyList())).thenReturn(Collections.emptyList());
        when(taxService.findAllByIdIn(anyList())).thenReturn(taxes);
        // default: no correction
        when(cacheService.isServerCorrectionEnabled(anyString())).thenReturn(false);
    }

    private Customer buildCustomer(String id) {
        Customer c = new Customer();
        c.setId(id);
        return c;
    }

    private Partner buildPartner(String id) {
        Partner p = new Partner();
        p.setId(id);
        return p;
    }
}
