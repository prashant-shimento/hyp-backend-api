package com.hyp.model;

import lombok.Data;

@Data
public class PlaceData {

	private String id;
	private String formattedAddress;
	private Location location;
}
