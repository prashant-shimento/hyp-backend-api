package com.hyp.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hyp.enums.SubscriptionPlan;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public class SubscriptionRequest {
    @NotNull(message = "subscriptionStart date is required")
    private LocalDateTime subscriptionStart;

    @NotNull(message = "subscriptionPlan is needed")
    private SubscriptionPlan subscriptionPlan;
}
