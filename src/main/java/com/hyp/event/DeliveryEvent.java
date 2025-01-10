package com.hyp.event;

import org.springframework.context.ApplicationEvent;

import com.hyp.entity.Delivery;
import lombok.Getter;

@Getter
public class DeliveryEvent extends ApplicationEvent {

	private static final long serialVersionUID = 1L;
	
	private final Delivery delivery;

	public DeliveryEvent(Object source, Delivery delivery) {
		super(source);
		this.delivery = delivery;
	}

}
