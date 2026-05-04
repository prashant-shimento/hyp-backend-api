package com.hyp.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusUpdateRequest {

    @NotBlank(message = "order_no is required")
    @JsonProperty("order_no")
    private String orderNo;

    @NotBlank(message = "new_status is required")
    @JsonProperty("new_status")
    private String newStatus;

    @JsonProperty("reason")
    private String reason;
}
