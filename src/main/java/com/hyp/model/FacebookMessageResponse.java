package com.hyp.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FacebookMessageResponse {

	@JsonProperty("messaging_product")
	private String messagingProduct;
	private List<Contacts> contacts;
	private List<Messages> messages;

	@Builder
	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class Contacts {
		private String input;
		@JsonProperty("wa_id")
		private String waId;
	}

	@Builder
	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class Messages {
		private String id;
		@JsonProperty("message_status")
		private String messageStatus;
	}
}
