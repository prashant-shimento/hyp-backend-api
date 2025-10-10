package com.hyp.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(Include.NON_NULL)
public class PosResponse {
    private Integer code;

    @JsonProperty("http_code")
    private Integer httpCode;

    private String message;
    private String status;

    @JsonProperty("store_status")
    private String storeStatus;

    private String error;
    private String success;
}
