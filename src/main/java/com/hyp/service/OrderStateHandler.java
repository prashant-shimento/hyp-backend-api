package com.hyp.service;

import com.hyp.entity.Order;

public interface OrderStateHandler {

	void createOrder(Order order);
    void processPayment(Order order);
    void processOrder(Order order);
    void dispatchOrder(Order order);
    void deliverOrder(Order order);
    void cancelOrder(Order order);
}
