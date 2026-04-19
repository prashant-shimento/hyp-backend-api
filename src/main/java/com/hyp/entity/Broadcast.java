package com.hyp.entity;

import com.hyp.enums.BroadcastScope;
import com.hyp.enums.BroadcastType;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "broadcasts")
public class Broadcast {

    @Id
    @Field("id")
    private String id;

    private BroadcastScope scope;

    @Field("receiver_id")
    private String receiverId;

    private BroadcastType type;

    private String message;

    private boolean active;

    @Field("triggered_by")
    private String triggeredBy;

    @Field("expires_at")
    private LocalDateTime expiresAt;

    @Field("cleared_at")
    private LocalDateTime clearedAt;

    @Field("created_at")
    @CreatedDate
    private LocalDateTime createdAt;
}
