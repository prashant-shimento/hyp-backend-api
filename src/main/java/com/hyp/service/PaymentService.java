package com.hyp.service;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.hyp.constants.Constants;
import com.hyp.entity.Payment;
import com.hyp.enums.OrderStatusType;
import com.hyp.repository.PaymentRepository;
import com.hyp.util.CommonUtils;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;

@Component
public class PaymentService extends BaseServiceImpl<Payment, String> {

	@Value("${razorpay.key}")
    private String razorPayKey;
	
	@Value("${razorpay.secret}")
    private String razorPaySecret;
	
	@Autowired
	PaymentRepository paymentRepository;
	
	@Autowired
	OrderService orderService;
	
	public Payment createPaymentOrder(String orderId, double amount) {
		try {
			RazorpayClient razorpayClient = new RazorpayClient(razorPayKey, razorPaySecret);
			JSONObject orderRequest = new JSONObject();
			orderRequest.put("amount", CommonUtils.getISOAmount(amount));
			orderRequest.put("currency", "INR");
			orderRequest.put("receipt", CommonUtils.genId());
			Order order = razorpayClient.orders.create(orderRequest);
			
			Payment payment = new Payment();
			payment.setId(CommonUtils.genId());
			payment.setPaymentOrderId(order.get("id"));
			payment.setAmount(Double.parseDouble(order.get("amount").toString()));
			payment.setReceipt(order.get("receipt"));
			payment.setStatus(order.get("status"));
			payment.setCurrency(order.get("currency"));
			payment.setProvider(Constants.RAZOR_PAY);
			payment.setOrderId(orderId);
			orderService.updateOrderStatus(orderId,OrderStatusType.PAYMENT_PENDING);
			return payment;
		} catch (Exception e) {
			e.printStackTrace();
            throw new RuntimeException("Error creating payment order: " + e.getMessage(), e);
		}	
	}
	
	public Payment findByPaymentOrderId(String paymentOrderId) {
		return paymentRepository.findByPaymentOrderId(paymentOrderId);
	}

}
