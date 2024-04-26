package com.hyp.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Data
@NoArgsConstructor
public class PosRiderUpdateRequest extends PosBaseRequest {
	
	@JsonProperty("order_id")
	private String orderId;
	
	@JsonProperty("outlet_id")
	private String restaurantId;
	
	@JsonProperty("external_order_id")
	private String externalOrderId;
	
	@JsonProperty("rider_data")
	private RiderDetails riderData;
	
	private String status;
	
	@Getter
	@Setter
	@AllArgsConstructor
	@ToString
	public static class RiderDetails {
		@JsonProperty("rider_name")
		private String riderName;
		@JsonProperty("rider_phone_number")
		private String riderContact;
	}
}
