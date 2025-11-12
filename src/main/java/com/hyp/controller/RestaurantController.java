package com.hyp.controller;

import com.hyp.dto.RestaurantDto;
import com.hyp.entity.Restaurant;
import com.hyp.exception.EntityNotFoundException;
import com.hyp.request.SettlementRequest;
import com.hyp.response.Response;
import com.hyp.service.RestaurantService;
import com.hyp.service.SettlementService;
import com.hyp.translation.RestaurantTranslation;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/restaurant")
public class RestaurantController extends BaseListController<RestaurantDto, Restaurant, String> {

    @Autowired
    public RestaurantTranslation restaurantTranslation;

    @Autowired
    private SimpMessagingTemplate messageTemplate;

    @Autowired
    RestaurantService restaurantService;

    @Autowired
    SettlementService settlementService;

    @PatchMapping("/{restaurantId}")
    public ResponseEntity<Response> updateRestaurant(
            @PathVariable String restaurantId, @RequestBody RestaurantDto restaurantDto) {

        try {
            Restaurant existingRestaurant = restaurantService.findById(restaurantId);
            if (existingRestaurant == null) {
                String message = "Restaurant not found with ID: " + restaurantId;
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Response(null, true, message));
            }
            restaurantTranslation.updateEntityFromDto(restaurantDto, existingRestaurant);
            Restaurant restaurant = restaurantService.save(existingRestaurant);
            RestaurantDto restaurantData = restaurantTranslation.getDto(restaurant);
            messageTemplate.convertAndSend("/topic/restaurant-serviceable", restaurantData);
            Response response =
                    new Response(Collections.singletonList(restaurantData), false, "Restaurant updated successfully");
            return ResponseEntity.ok(response);

        } catch (Exception ex) {
            Response response = new Response(null, true, ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/{restaurantId}/settlement")
    public ResponseEntity<Response> orderSettlement(
            @PathVariable String restaurantId,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate)
            throws EntityNotFoundException {
        log.info("Computing Settlement for restaurantId {} from {} to {}", restaurantId, startDate, endDate);

        Optional.ofNullable(restaurantService.findById(restaurantId))
                .orElseThrow(() -> new EntityNotFoundException("Restaurant", restaurantId));

        SettlementRequest request = SettlementRequest.builder()
                .restaurantId(restaurantId)
                .startDate(startDate)
                .endDate(endDate)
                .build();
        settlementService.processSettlement(request);

        return ResponseEntity.ok(Response.builder()
                .error(false)
                .message("Settlement request will be processed")
                .build());
    }
}
