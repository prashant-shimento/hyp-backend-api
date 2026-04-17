package com.hyp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.LatLng;
import com.hyp.dto.AddressDto;
import com.hyp.entity.Restaurant;
import com.hyp.model.PlacePredictionData;
import com.hyp.request.PredictionRequest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class LocationService {

    @Value("${google.api.key}")
    private String googleApiKey;

    @Autowired
    private RedisService redisService;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private ObjectMapper objectMapper;

    public boolean isLocationDeliverable(
            double userLatitude,
            double userLongitude,
            double restaurantLatitude,
            double restaurantLongitude,
            double radius) {
        double distance = calculateDistance(userLatitude, userLongitude, restaurantLatitude, restaurantLongitude);
        return distance <= radius;
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double earthRadius = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                        * Math.cos(Math.toRadians(lat2))
                        * Math.sin(dLon / 2)
                        * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }

    public PlacePredictionData getLocationPrediction(String search) {
        String key = "google:map:search:" + search;

        Optional<PlacePredictionData> cachedData = redisService.getRedisJsonData(key, PlacePredictionData.class);
        if (cachedData.isPresent()) {
            redisService.increment("metrics:places:autocomplete:cache_hits");
            log.info("Returning cached PlacePredictionData for key: {}", key);
            return cachedData.get();
        }

        try {
            String sessionToken = UUID.randomUUID().toString();
            PredictionRequest predictionRequest = PredictionRequest.builder()
                    .input(search)
                    // .sessionToken(sessionToken)
                    .includedRegionCodes(List.of("in"))
                    .locationBias(PredictionRequest.LocationBias.builder()
                            .circle(PredictionRequest.LocationBiasCircle.builder()
                                    .radius(50000)
                                    .center(PredictionRequest.LatLng.builder()
                                            .latitude(17.4065)
                                            .longitude(78.4772)
                                            .build())
                                    .build())
                            .build())
                    .build();
            String apiUrl = "https://places.googleapis.com/v1/places:autocomplete";

            WebClient webClient = WebClient.builder()
                    .baseUrl(apiUrl)
                    .defaultHeader("X-Goog-Api-Key", googleApiKey)
                    .defaultHeader(
                            "X-Goog-FieldMask",
                            "suggestions.placePrediction.placeId,suggestions.placePrediction.text.text")
                    .build();
            PlacePredictionData placePredictionResponse = webClient
                    .post()
                    .body(BodyInserters.fromValue(predictionRequest))
                    .retrieve()
                    .bodyToMono(PlacePredictionData.class)
                    .doOnError(e ->
                            log.error("Error in getLocationPrediction on search {} : {}", search, e.getMessage(), e))
                    .block();
            redisService.increment("metrics:places:autocomplete:api_hits");
            if (placePredictionResponse != null) {
                redisService.setRedisJsonData(
                        key, placePredictionResponse, Duration.ofMinutes(60).toSeconds());

                // TODO: Next session
                //				placePredictionResponse.getSuggestions().forEach(suggestion -> {
                //						String placeId = suggestion.getPlacePrediction().getPlaceId();
                //						if (placeId != null) {
                //							String tokenKey = "google:map:session:" + placeId;
                //							redisService.setRedisData(tokenKey, sessionToken, Duration.ofMinutes(5).toSeconds());
                //						}
                //				});
            }

            return placePredictionResponse;
        } catch (Exception e) {
            log.error("Error in getLocationPrediction {}", e.getMessage());
            throw new RuntimeException("Error in getLocationPrediction: " + e.getMessage(), e);
        }
    }

    public String getPlaceDetails(String placeId) {
        return cacheService.getOrLoad("placeDetails", placeId, String.class, () -> fetchPlaceDetailsFromApi(placeId));
    }

    private String fetchPlaceDetailsFromApi(String placeId) {
        try {
            String url = "https://places.googleapis.com/v1/places/" + placeId;
            WebClient.Builder builder = WebClient.builder()
                    .baseUrl(url)
                    .defaultHeader("X-Goog-Api-Key", googleApiKey)
                    .defaultHeader("X-Goog-FieldMask", "addressComponents,location");

            WebClient webClient = builder.build();
            Mono<String> placeResponse = webClient
                    .get()
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnError(
                            e -> log.error("Error in getPlaceDetails on placeId {} : {}", placeId, e.getMessage(), e));
            String response = placeResponse.block();
            redisService.increment("metrics:places:details:api_hits");
            return response;
        } catch (Exception e) {
            log.error("Error in getPlaceDetails {}", e.getMessage());
            throw new RuntimeException("Error in getPlaceDetails: " + e.getMessage(), e);
        }
    }

    public GeocodingResult[] getPlaceByGeocodeByClient(double latitude, double longitude) {
        try {
            GeoApiContext context =
                    new GeoApiContext.Builder().apiKey(googleApiKey).build();
            LatLng latLng = new LatLng(latitude, longitude);
            return GeocodingApi.reverseGeocode(context, latLng).await();
        } catch (Exception e) {
            log.error("Error in getPlaceByGeocodeByClient {}", e.getMessage());

            throw new RuntimeException("Error in getPlaceByGeocodeByClient: " + e.getMessage(), e);
        }
    }

    public List<String> getServiceableRestaurants(AddressDto addressPlaceData, List<Restaurant> restaurants) {
        List<String> serviceableRestaurants = new ArrayList<>();
        try {
            for (Restaurant restaurant : restaurants) {
                if (this.isLocationDeliverable(
                        addressPlaceData.getLocation().getLatitude(),
                        addressPlaceData.getLocation().getLongitude(),
                        restaurant.getLocation().getLatitude(),
                        restaurant.getLocation().getLongitude(),
                        restaurant.getDeliveryRadius())) {
                    serviceableRestaurants.add(restaurant.getId());
                }
            }
            return serviceableRestaurants;
        } catch (Exception e) {
            log.error("Error in getServiceableRestaurants {}", e.getMessage());
            throw new RuntimeException("Error in getServiceableRestaurants: " + e.getMessage(), e);
        }
    }
}
