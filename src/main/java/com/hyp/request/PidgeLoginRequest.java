package com.hyp.request;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PidgeLoginRequest {
    private String username;
    private String password;
}
