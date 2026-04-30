package com.hyp.config;

import com.hyp.controller.BaseController;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ScopingConfig {

    @Value("${security.restaurant-scoping.enabled:false}")
    private boolean restaurantScopingEnabled;

    @Value("${security.restaurant-scoping.strict:false}")
    private boolean strictMode;

    @Bean
    public BaseController.ScopingMode defaultScopingMode() {
        if (!restaurantScopingEnabled) {
            return BaseController.ScopingMode.LEGACY;
        }
        return strictMode ? BaseController.ScopingMode.STRICT_SCOPED : BaseController.ScopingMode.SMART;
    }
}
