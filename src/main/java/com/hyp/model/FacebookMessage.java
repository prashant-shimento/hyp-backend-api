package com.hyp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FacebookMessage {

	private String messaging_product;
	private String recipient_type;
	private String to;
	private String type;
	private Template template;

	@Builder
	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class Template {
		private String name;
		private Language language;

		@Builder
		@Data
		@AllArgsConstructor
		@NoArgsConstructor
		public static class Language {
			private String code;
		}
	}
}
