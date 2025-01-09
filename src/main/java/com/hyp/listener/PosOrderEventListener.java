package com.hyp.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.hyp.entity.Order;
import com.hyp.event.PosOrderEvent;
import com.hyp.service.PosService;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class PosOrderEventListener {

	@Autowired
	PosService posService;

	@Async
	@EventListener
	public void handleProcessPosOrder(PosOrderEvent event) {
		log.info("Pos Order Event listener handleProcessPosOrder");
		Order order = event.getOrder();
		posService.processPosOrder(order);
	}

}
