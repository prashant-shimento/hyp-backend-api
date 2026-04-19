package com.hyp.request;

import com.hyp.enums.BroadcastScope;
import com.hyp.enums.BroadcastType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BroadcastRequest {

    @NotNull
    private BroadcastScope scope;

    @NotBlank
    private String receiverId;

    @NotNull
    private BroadcastType type;

    @NotBlank
    private String message;

    @Positive(message = "ttlMinutes must be a positive integer")
    private Integer ttlMinutes;
}
