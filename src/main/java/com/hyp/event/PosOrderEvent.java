package com.hyp.event;

import org.springframework.context.ApplicationEvent;

import com.hyp.entity.Order;

import lombok.Getter;

@Getter
public class PosOrderEvent extends ApplicationEvent {

	private static final long serialVersionUID = 1L;
	
	private final Order order;

	public PosOrderEvent(Object source, Order order) {
		super(source);
		this.order = order;
	}

}
