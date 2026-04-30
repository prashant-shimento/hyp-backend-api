package com.hyp.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.hyp.annotation.GenerateId;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Document(collection = "integration_keys")
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(Include.NON_NULL)
@Builder
public class IntegrationKey {

    @Id
    @Field("id")
    @GenerateId()
    private String id;

    @Field("name")
    private String name; // e.g., "PetPooja POS", "Pidge Delivery", "Razorpay Webhook"

    @Field("api_key")
    @Indexed(unique = true)
    private String apiKey; // The API key (should be hashed for security)

    @Field("secret")
    private String secret; // HMAC secret for signature validation (encrypted) - optional

    @Field("allowed_ips")
    private Set<String> allowedIps; // IP whitelist for this API key (optional)

    @Field("require_ip_validation")
    @Builder.Default
    private boolean requireIpValidation = false; // If true, requests must come from allowed IPs

    @Field("auth_mode")
    @Builder.Default
    private AuthMode authMode = AuthMode.STATIC_TOKEN; // Authentication mode

    @Field("partner_id")
    @Indexed
    private String partnerId; // Reference to Partner entity

    @Field("restaurant_id")
    private String restaurantId; // For restaurant-specific webhooks

    @Field("webhook_type")
    private WebhookType webhookType;

    @Field("role")
    private String role; // Role to assign when authenticated (e.g., POS_PARTNER)

    @Field("active")
    @Builder.Default
    private boolean active = true;

    @Field("last_used_at")
    private LocalDateTime lastUsedAt;

    @Field("created_at")
    @CreatedDate
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Field("updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;

    public enum WebhookType {
        POS_CALLBACK,
        DELIVERY_CALLBACK,
        PAYMENT_CALLBACK
    }

    public enum AuthMode {
        STATIC_TOKEN, // Just API key validation (with optional IP restriction)
        HMAC_SIGNATURE // API key + HMAC signature validation
    }
}
