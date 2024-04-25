package com.hyp.constants;

import java.util.Arrays;
import java.util.List;

public interface Constants {

	String RAZOR_PAY = "RAZOR_PAY";
	int VENDOR_HANDLE_DELIVERY = 0;
	int RESTAURANT_HANDLE_DELIVERY = 1;
	String GST_VENDOR_LIABLE = "vendor";
	String PET_POOJA = "PET_POOJA";
	String PIDGE = "PIDGE";
	List<String> CUSTOMER_PARAMS = Arrays.asList("id","name","mobile","createdAt");
	List<String> ORDER_PARAMS = Arrays.asList("id","status","customerId","createdAt","totalAmount","orderType","orderTime");
}
