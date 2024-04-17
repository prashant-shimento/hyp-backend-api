package com.hyp.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class DeliveryQuote {
	private QuoteData data;

	@Data
	public static class QuoteData {
		private List<Distance> distance;
		private List<DeliveryNetworks> items;
	}

	@Data
	public static class Distance {
		private String ref;
		private double distance;
	}

	@Data
	public static class DeliveryNetworks {
		@JsonProperty("network_id")
		private int networkId;

		@JsonProperty("network_name")
		private String networkName;

		private String service;

		@JsonProperty("pickup_now")
		private boolean pickupNow;
		
		private boolean manifest;

		private Quote quote;

		private String error;
		
		private String token;
	}

	@Data
	public static class Quote {
		private double price;
		@JsonProperty("price_breakup")
		private QuotePriceBreakup priceBreakup;
		private Eta eta;
	}

	@Data
	public static class QuotePriceBreakup {
		@JsonProperty("base_delivery_charge")
		private double baseDeliveryCharge;

		@JsonProperty("total_gst_amount")
		private double totalGstAmount;

		private double surge;

		@JsonProperty("additional_charges")
		private List<AdditionalCharge> additionalCharges;

		private List<QuoteItem> items;
	}

	@Data
	public static class QuoteItem {
		private double amount;
		private double tax;
		private double total;

		@JsonProperty("order_id")
		private String orderId;
	}

	@Data
	public static class Eta {
		private String pickup;
		@JsonProperty("pickup_min")
		private String pickupMin;
		private String drop;
		@JsonProperty("drop_min")
		private String dropMin;
	}
	
	@Data
	public static class AdditionalCharge {
		private String type;
		private String value;
		private String details;
	}
}
