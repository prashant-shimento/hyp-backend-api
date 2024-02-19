package com.hyp.service;

import org.springframework.stereotype.Service;

import com.hyp.request.MenuRequest;

@Service
public interface PushMenuService {

	public boolean pushMenu(MenuRequest pushMenuRequest);
}
