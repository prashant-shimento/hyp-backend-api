package com.hyp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiConfig {

    private String baseUrl;

    private String token;

    private String secret;

    private String key;

    private String username;

    private String password;
}
