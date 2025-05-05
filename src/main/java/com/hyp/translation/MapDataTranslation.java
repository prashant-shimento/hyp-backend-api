package com.hyp.translation;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.maps.model.AddressComponent;
import com.google.maps.model.AddressComponentType;
import com.google.maps.model.AddressType;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.LatLng;
import com.hyp.dto.AddressDto;
import com.hyp.model.Location;

@Component
public class MapDataTranslation {

	private static final ObjectMapper mapper = new ObjectMapper();

	public static AddressDto getPlaceDatatoAddress(String rawPlaceData) {

		JsonNode jsonNode;
		AddressDto address = null;

		try {
			jsonNode = mapper.readTree(rawPlaceData);
			JsonNode addressComponents = jsonNode.get("addressComponents");
			JsonNode location = jsonNode.get("location");

			address = new AddressDto();
			address.setLocation(
					new Location(location.get("latitude").asDouble(), location.get("longitude").asDouble()));

			for (JsonNode component : addressComponents) {
				String longText = component.get("longText").asText();
				String[] types = mapper.convertValue(component.get("types"), String[].class);

				for (String type : types) {
					switch (type) {
					case "street_number":
						address.setAddressOne(longText);
						break;
					case "route":
						address.setAddressOne(
								address.getAddressOne() != null ? address.getAddressOne() + " , " + longText
										: longText);
						break;
					case "sublocality_level_3":
						address.setAddressOne(
								address.getAddressOne() != null ? address.getAddressOne() + " , " + longText
										: longText);
						break;
					case "sublocality_level_2":
						address.setAddressTwo(longText);
						break;
					case "sublocality_level_1":
						address.setAddressTwo(
								address.getAddressTwo() != null ? address.getAddressTwo() + " , " + longText
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
			e.printStackTrace();
		}
		return address;
	}

	public static AddressDto getGeocodeDatatoAddress(GeocodingResult[] results) {
		AddressDto address = null;
		boolean premiseFound = false;
		boolean streetAddressFound = false;
		try {
			for (GeocodingResult geocodingResult : results) {
				AddressType[] addressTypes = geocodingResult.types;
				for (AddressType addressType : addressTypes) {
					if (addressType.equals(AddressType.PREMISE) || addressType.equals(AddressType.STREET_ADDRESS)) {
						premiseFound = premiseFound || addressType.equals(AddressType.PREMISE);
						streetAddressFound = streetAddressFound || addressType.equals(AddressType.STREET_ADDRESS);
						AddressComponent[] addressComponents = geocodingResult.addressComponents;
						LatLng location = geocodingResult.geometry.location;
						address = new AddressDto();
						address.setLocation(new Location(location.lat, location.lng));
						for (AddressComponent addressComponent : addressComponents) {
							String longText = addressComponent.longName;
							AddressComponentType[] addressComponentTypes = addressComponent.types;
							for (AddressComponentType addressComponentType : addressComponentTypes) {
								switch (addressComponentType) {
								case STREET_NUMBER:
									address.setAddressOne(addressComponent.longName);
									break;
								case PREMISE:
									address.setAddressOne(
											address.getAddressOne() != null ? address.getAddressOne() + " , " + longText
													: longText);
									break;
								case ROUTE:
									address.setAddressOne(
											address.getAddressOne() != null ? address.getAddressOne() + " , " + longText
													: longText);
									break;
								case SUBLOCALITY_LEVEL_3:
									address.setAddressOne(
											address.getAddressOne() != null ? address.getAddressOne() + " , " + longText
													: longText);
									break;
								case SUBLOCALITY_LEVEL_2:
									address.setAddressTwo(longText);
									break;
								case SUBLOCALITY_LEVEL_1:
									address.setAddressTwo(
											address.getAddressTwo() != null ? address.getAddressTwo() + " , " + longText
													: longText);
									break;
								case NEIGHBORHOOD:
									address.setLandmark(longText);
									break;
								case LOCALITY:
									address.setCity(longText);
									break;
								case ADMINISTRATIVE_AREA_LEVEL_1:
									address.setState(longText);
									break;
								case COUNTRY:
									address.setCountry(longText);
									break;
								case POSTAL_CODE:
									address.setPincode(longText);
									break;
								default:
									break;
								}

							}
						}
					}
					LatLng location = geocodingResult.geometry.location;
					address = new AddressDto();
					address.setLocation(new Location(location.lat, location.lng));
				}
				if (premiseFound && streetAddressFound) {
					break;
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return address;
	}

}
