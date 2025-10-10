package com.hyp.translation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.maps.model.AddressComponent;
import com.google.maps.model.AddressComponentType;
import com.google.maps.model.AddressType;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.LatLng;
import com.hyp.dto.AddressDto;
import com.hyp.model.Location;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MapDataTranslation {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static AddressDto getPlaceDataToAddress(String rawPlaceData) {

        JsonNode jsonNode;
        AddressDto address = null;

        try {
            jsonNode = mapper.readTree(rawPlaceData);
            JsonNode addressComponents = jsonNode.get("addressComponents");
            JsonNode location = jsonNode.get("location");

            address = new AddressDto();
            address.setLocation(new Location(
                    location.get("latitude").asDouble(),
                    location.get("longitude").asDouble()));

            for (JsonNode component : addressComponents) {
                String longText = component.get("longText").asText();
                String[] types = mapper.convertValue(component.get("types"), String[].class);

                for (String type : types) {
                    switch (type) {
                        case "street_number":
                            address.setAddressOne(longText);
                            break;
                        case "route", "sublocality_level_3":
                            address.setAddressOne(
                                    address.getAddressOne() != null
                                            ? address.getAddressOne() + " , " + longText
                                            : longText);
                            break;
                        case "sublocality_level_2":
                            address.setAddressTwo(longText);
                            break;
                        case "sublocality_level_1":
                            address.setAddressTwo(
                                    address.getAddressTwo() != null
                                            ? address.getAddressTwo() + " , " + longText
                                            : longText);
                            break;
                        case "neighborhood":
                            address.setLandmark(longText);
                            break;
                        case "locality":
                            address.setCity(longText);
                            break;
                        case "administrative_area_level_1":
                            address.setState(longText);
                            break;
                        case "country":
                            address.setCountry(longText);
                            break;
                        case "postal_code":
                            address.setPincode(longText);
                            break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("Exception occurred on getPlaceDataToAddress {}", e.getMessage());
        }
        return address;
    }

    public static AddressDto getGeocodeDataToAddress(GeocodingResult[] results) {
        if (results == null || results.length == 0) return null;

        AddressDto selectedAddress = null;

        try {
            for (GeocodingResult result : results) {
                boolean premiseFound = false;
                boolean streetAddressFound = false;
                AddressDto address = new AddressDto();

                // Set location
                if (result.geometry != null && result.geometry.location != null) {
                    LatLng location = result.geometry.location;
                    address.setLocation(new Location(location.lat, location.lng));
                }

                for (AddressType type : result.types) {
                    if (type == AddressType.PREMISE) premiseFound = true;
                    if (type == AddressType.STREET_ADDRESS) streetAddressFound = true;
                }

                if (result.addressComponents != null) {
                    for (AddressComponent component : result.addressComponents) {
                        String longName = component.longName;
                        if (component.types == null) continue;

                        for (AddressComponentType type : component.types) {
                            switch (type) {
                                case STREET_NUMBER -> address.setAddressOne(longName);
                                case PREMISE, SUBLOCALITY_LEVEL_3, ROUTE -> {
                                    String addr1 = address.getAddressOne();
                                    address.setAddressOne(addr1 != null ? addr1 + ", " + longName : longName);
                                }
                                case SUBLOCALITY_LEVEL_2 -> address.setAddressTwo(longName);
                                case SUBLOCALITY_LEVEL_1 -> {
                                    String addr2 = address.getAddressTwo();
                                    address.setAddressTwo(addr2 != null ? addr2 + ", " + longName : longName);
                                }
                                case NEIGHBORHOOD -> address.setLandmark(longName);
                                case LOCALITY -> address.setCity(longName);
                                case ADMINISTRATIVE_AREA_LEVEL_1 -> address.setState(longName);
                                case COUNTRY -> address.setCountry(longName);
                                case POSTAL_CODE -> address.setPincode(longName);
                            }
                        }
                    }
                }

                if (selectedAddress == null || (premiseFound && streetAddressFound)) {
                    selectedAddress = address;
                }

                if (premiseFound && streetAddressFound) {
                    break;
                }
            }
        } catch (Exception e) {
            log.error("Exception occurred on getGeocodeDataToAddress {}", e.getMessage());
        }

        return selectedAddress;
    }
}
