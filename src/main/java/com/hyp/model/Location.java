package com.hyp.model;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Location {

	@NotNull(message = "Latitude is mandatory")
	private double latitude;
	@NotNull(message = "Longitude is mandatory")
	private double longitude;
}
