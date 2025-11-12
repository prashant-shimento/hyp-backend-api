package com.hyp.service;

import com.hyp.entity.Fee;
import com.hyp.repository.FeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FeeService extends BaseServiceImpl<Fee, String> {

    @Autowired
    FeeRepository feeRepository;

    public Fee findByRestaurantId(String restaurantId) {
        return feeRepository.findByRestaurantId(restaurantId);
    }
}
