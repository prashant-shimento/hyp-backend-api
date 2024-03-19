package com.hyp.service;

import java.util.UUID;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.hyp.util.CommonUtils;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;

@Component
public class PaymentServiceImpl implements PaymentService {

	@Value("${razorpay.key}")
    private String razorPayKey;
	
	@Value("${razorpay.secret}")
    private String razorPaySecret;
	
	@Override
	public Order createPaymentOrder(double amount) {
		String orderId = null;
		try {
			RazorpayClient razorpayClient = new RazorpayClient(razorPayKey, razorPaySecret);
			JSONObject orderRequest = new JSONObject();
			orderRequest.put("amount", CommonUtils.getISOAmount(amount));
			orderRequest.put("currency", "INR");
			orderRequest.put("receipt", UUID.randomUUID());
			return razorpayClient.orders.create(orderRequest);
		} catch (RazorpayException e) {
			e.printStackTrace();
			return null;
		}
		
	}

}
