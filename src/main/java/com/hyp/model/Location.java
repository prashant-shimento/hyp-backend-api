package com.hyp.model;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Location {

	@NotNull(message = "Latitude is mandatory")
	private Double latitude;
	@NotNull(message = "Longitude is mandatory")
	private Double longitude;
}
