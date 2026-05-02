package com.hyp.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.hyp.annotation.GenerateId;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Document(collection = "refresh_tokens")
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(Include.NON_NULL)
@Builder
public class RefreshToken {

    @Id
    @Field("id")
    @GenerateId()
    private String id;

    @Field("token")
    @Indexed(unique = true)
    private String token; // Hashed refresh token

    @Field("user_id")
    @Indexed
    private String userId;

    @Field("user_type")
    private String userType; // CUSTOMER, USER, PARTNER

    @Field("role")
    private String role;

    @Field("restaurant_id")
    private String restaurantId;

    @Field("expires_at")
    @Indexed(expireAfterSeconds = 0) // TTL index - auto delete expired tokens
    private LocalDateTime expiresAt;

    @Field("revoked")
    @Builder.Default
    private boolean revoked = false;

    @Field("revoked_at")
    private LocalDateTime revokedAt;

    @Field("device_info")
    private String deviceInfo;

    @Field("ip_address")
    private String ipAddress;

    @Field("created_at")
    @CreatedDate
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !revoked && !isExpired();
    }
}
