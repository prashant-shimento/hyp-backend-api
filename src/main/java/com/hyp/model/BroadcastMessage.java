package com.hyp.model;

import com.hyp.enums.BroadcastType;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BroadcastMessage {

    private String broadcastId;
    private BroadcastType type;
    /** ACTIVE or CLEARED */
    private String status;

    private String message;
    private String triggeredBy;
    private LocalDateTime expiresAt;
}
