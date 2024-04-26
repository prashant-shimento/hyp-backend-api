package com.hyp.dto;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class RazorpayEventDto {

	public String entity;
	public String account_id;
	public String event;
	public List<String> contains;
	public Payload payload;
	public int created_at;

	@Getter
	@Setter
	@NoArgsConstructor
	@ToString
	public static class PaymentEntity {
		public String id;
		public String entity;
		public double amount;
		public String currency;
		public String status;
		public String order_id;
		public String invoice_id;
		public boolean international;
		public String method;
		public int amount_refunded;
		public String refund_status;
		public boolean captured;
		public String description;
		public String card_id;
		public String bank;
		public String wallet;
		public String vpa;
		public String email;
		public String contact;
		public Object notes;
		public int fee;
		public int tax;
		public String error_code;
		public String error_description;
		public String error_source;
		public String error_step;
		public String error_reason;
		public AcquirerData acquirer_data; 
		public int created_at;
	}

	@Getter
	@Setter
	@NoArgsConstructor
	@ToString
	public static class Payment {
		public PaymentEntity entity;
	}
	
	@Getter
	@Setter
	@NoArgsConstructor
	@ToString
	public static class AcquirerData {
		public String bank_transaction_id;
		public String arn;
	}
	

	@Getter
	@Setter
	@NoArgsConstructor
	@ToString
	public static class OrderEntity {
		public String id;
		public String entity;
		public int amount;
		public int amount_paid;
		public int amount_due;
		public String currency;
		public String receipt;
		public String offer_id;
		public String status;
		public int attempts;
		public Object notes;
		public int created_at;
	}

	@Getter
	@Setter
	@NoArgsConstructor
	@ToString
	public static class Order {
		public OrderEntity entity;
	}
	
	@Getter
	@Setter
	@NoArgsConstructor
	@ToString
	public static class Refund {
		public RefundEntity entity;
	}
	
	@Getter
	@Setter
	@NoArgsConstructor
	@ToString
	public static class RefundEntity {
		public String id;
		public String entity;
		public int amount;
		public String currency;
		public String payment_id;
		public Object notes;
		public AcquirerData acquirer_data; 
		public String receipt;
		public String batch_id;
		public String status;
		public String speed_processed;
		public String speed_requested;
		public int created_at;
	}

	@Getter
	@Setter
	@NoArgsConstructor
	@ToString
	public static class Payload {
		public Payment payment;
		public Order order;
		public Refund refund;
	}

}
