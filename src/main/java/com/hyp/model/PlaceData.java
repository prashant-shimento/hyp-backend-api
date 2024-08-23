package com.hyp.model;

import java.util.List;

import com.hyp.dto.AddressDto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlaceData {

	private AddressDto address;
	private List<String> restaurants;
}
