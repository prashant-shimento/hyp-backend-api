package com.hyp.service;

import com.hyp.entity.Customer;
import com.hyp.entity.Order;
import com.hyp.entity.Restaurant;
import com.hyp.enums.PartnerType;
import com.hyp.exception.PosException;
import com.hyp.exception.RequestTranslationException;
import com.hyp.request.PosOrderRequest;
import com.hyp.request.PosRiderUpdateRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Optional;

@Slf4j
@Service("petPooja")
public class PetPoojaServiceImpl extends PosServiceImpl {

    @Value("${pos.petpooja.url}")
    private String baseUrl;

    public PetPoojaServiceImpl(
            AttributeService attributeService,
            CategoryService categoryService,
            TaxService taxService,
            OrderTypeService orderTypeService,
            VariationService variationService,
            RestaurantService restaurantService,
            DiscountService discountService,
            AddonGroupService addonGroupService,
            AddonItemService addonItemService,
            ItemService itemService,
            BucketService bucketService) {
        super(attributeService, categoryService, taxService, orderTypeService, variationService,
                restaurantService, discountService, addonGroupService, addonItemService, itemService, bucketService);
    }

    @Override
    protected String posBaseUrl() {
        return baseUrl;
    }

    @Override
    public void processPosOrder(Order order) {
        Customer customer = customerService.findById(order.getCustomerId());
        Restaurant restaurant = restaurantService.findById(order.getRestaurantId());
        try {
            PosOrderRequest posOrderRequest =
                    posOrderRequestTranslation.getPosOrderRequest(restaurant, order, customer);
            if (partnerService.findPartnersByRestaurantId(order.getRestaurantId(), PartnerType.THEATRE) != null) {
                String name = String.format(
                        "%s-%s-%s",
                        Optional.ofNullable(order.getScreen()).orElse("N/A"),
                        Optional.ofNullable(order.getSeat()).orElse("N/A"),
                        customer.getName());
                posOrderRequest
                        .getOrderInfo()
                        .getOrderInfoDetails()
                        .getCustomer()
                        .getCustomerDetails()
                        .setName(name);
            }
            createPosOrder(posOrderRequest);
        } catch (RequestTranslationException e) {
            log.error("Error occurred on RequestTranslationException for order {} cause: {}",
                    order.getId(), e.getMessage());
        } catch (PosException e) {
            log.error("Error occurred on PosException for order {} cause: {}", order.getId(), e.getMessage());
        }
    }

    @Override
    public String updatePosRiderStatus(PosRiderUpdateRequest posRiderUpdateRequest) {
        try {
            log.info("updatePosRiderStatus Request {}", objectMapper.writeValueAsString(posRiderUpdateRequest));
            WebClient webClient = WebClient.builder().baseUrl(baseUrl).build();
            String endpoint = "/rider_status_update";
            String riderUpdateResponse = webClient
                    .post()
                    .uri(endpoint)
                    .body(BodyInserters.fromValue(posRiderUpdateRequest))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            log.info("updatePosRiderStatus Response {}", objectMapper.writeValueAsString(riderUpdateResponse));
            return riderUpdateResponse;
        } catch (Exception e) {
            log.error("Error occurred during updatePosRiderStatus {}", e.getMessage());
            return null;
        }
    }
}
