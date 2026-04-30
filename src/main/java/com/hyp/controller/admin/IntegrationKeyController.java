package com.hyp.controller.admin;

import com.hyp.entity.IntegrationKey;
import com.hyp.entity.IntegrationKey.AuthMode;
import com.hyp.entity.IntegrationKey.WebhookType;
import com.hyp.exception.EntityNotFoundException;
import com.hyp.repository.IntegrationKeyRepository;
import com.hyp.response.Response;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/admin/api-keys")
@RequiredArgsConstructor
public class IntegrationKeyController {

    private final IntegrationKeyRepository integrationKeyRepository;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @GetMapping
    public ResponseEntity<Response> listApiKeys(
            @RequestParam(required = false) String webhookType,
            @RequestParam(required = false) String partnerId,
            @RequestParam(required = false) Boolean active) {

        List<IntegrationKey> integrationKeys;

        if (webhookType != null) {
            integrationKeys = integrationKeyRepository.findByWebhookType(WebhookType.valueOf(webhookType));
        } else if (partnerId != null) {
            integrationKeys = integrationKeyRepository.findByPartnerId(partnerId);
        } else if (Boolean.TRUE.equals(active)) {
            integrationKeys = integrationKeyRepository.findByActiveTrue();
        } else {
            integrationKeys = integrationKeyRepository.findAll();
        }

        List<Map<String, Object>> maskedKeys =
                integrationKeys.stream().map(this::maskResponse).toList();

        return ResponseEntity.ok(new Response(maskedKeys, false, "Integration keys retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Response> getApiKey(@PathVariable String id) throws EntityNotFoundException {
        IntegrationKey integrationKey = integrationKeyRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException("IntegrationKey", id));

        return ResponseEntity.ok(new Response(
                Collections.singletonList(maskResponse(integrationKey)),
                false,
                "Integration key retrieved successfully"));
    }

    @PostMapping
    public ResponseEntity<Response> createApiKey(@RequestBody @Valid CreateIntegrationKeyRequest request) {
        String generatedApiKey = generateApiKey(request.getKeyPrefix());

        IntegrationKey integrationKey = IntegrationKey.builder()
                .name(request.getName())
                .apiKey(generatedApiKey)
                .secret(request.getSecret())
                .allowedIps(request.getAllowedIps())
                .requireIpValidation(request.isRequireIpValidation())
                .authMode(request.getAuthMode() != null ? request.getAuthMode() : AuthMode.STATIC_TOKEN)
                .partnerId(request.getPartnerId())
                .restaurantId(request.getRestaurantId())
                .webhookType(request.getWebhookType())
                .role(request.getRole())
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        IntegrationKey saved = integrationKeyRepository.save(integrationKey);
        log.info("Created new integration key: {} for {}", saved.getId(), saved.getName());

        Map<String, Object> response = maskResponse(saved);
        response.put("apiKey", generatedApiKey); // Show full key only on creation
        response.put("warning", "Save this API key securely. It will not be shown again.");

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new Response(Collections.singletonList(response), false, "Integration key created successfully"));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Response> updateApiKey(
            @PathVariable String id, @RequestBody UpdateIntegrationKeyRequest request) throws EntityNotFoundException {

        IntegrationKey integrationKey = integrationKeyRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException("IntegrationKey", id));

        if (request.getName() != null) {
            integrationKey.setName(request.getName());
        }
        if (request.getSecret() != null) {
            integrationKey.setSecret(request.getSecret());
        }
        if (request.getAllowedIps() != null) {
            integrationKey.setAllowedIps(request.getAllowedIps());
        }
        if (request.getRequireIpValidation() != null) {
            integrationKey.setRequireIpValidation(request.getRequireIpValidation());
        }
        if (request.getAuthMode() != null) {
            integrationKey.setAuthMode(request.getAuthMode());
        }
        if (request.getPartnerId() != null) {
            integrationKey.setPartnerId(request.getPartnerId());
        }
        if (request.getRestaurantId() != null) {
            integrationKey.setRestaurantId(request.getRestaurantId());
        }
        if (request.getWebhookType() != null) {
            integrationKey.setWebhookType(request.getWebhookType());
        }
        if (request.getRole() != null) {
            integrationKey.setRole(request.getRole());
        }
        if (request.getActive() != null) {
            integrationKey.setActive(request.getActive());
        }

        integrationKey.setUpdatedAt(LocalDateTime.now());
        IntegrationKey saved = integrationKeyRepository.save(integrationKey);
        log.info("Updated integration key: {}", saved.getId());

        return ResponseEntity.ok(new Response(
                Collections.singletonList(maskResponse(saved)), false, "Integration key updated successfully"));
    }

    @PostMapping("/{id}/regenerate")
    public ResponseEntity<Response> regenerateApiKey(
            @PathVariable String id, @RequestParam(required = false, defaultValue = "key") String keyPrefix)
            throws EntityNotFoundException {

        IntegrationKey integrationKey = integrationKeyRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException("IntegrationKey", id));

        String newApiKey = generateApiKey(keyPrefix);
        integrationKey.setApiKey(newApiKey);
        integrationKey.setUpdatedAt(LocalDateTime.now());

        IntegrationKey saved = integrationKeyRepository.save(integrationKey);
        log.info("Regenerated integration key: {} for {}", saved.getId(), saved.getName());

        Map<String, Object> response = maskResponse(saved);
        response.put("apiKey", newApiKey); // Show full key on regeneration
        response.put("warning", "Save this API key securely. It will not be shown again.");

        return ResponseEntity.ok(
                new Response(Collections.singletonList(response), false, "Integration key regenerated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Response> deleteApiKey(@PathVariable String id) throws EntityNotFoundException {
        IntegrationKey integrationKey = integrationKeyRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException("IntegrationKey", id));

        integrationKeyRepository.deleteById(id);
        log.info("Deleted integration key: {} ({})", id, integrationKey.getName());

        return ResponseEntity.ok(new Response(null, false, "Integration key deleted successfully"));
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<Response> deactivateApiKey(@PathVariable String id) throws EntityNotFoundException {
        IntegrationKey integrationKey = integrationKeyRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException("IntegrationKey", id));

        integrationKey.setActive(false);
        integrationKey.setUpdatedAt(LocalDateTime.now());
        integrationKeyRepository.save(integrationKey);

        log.info("Deactivated integration key: {} ({})", id, integrationKey.getName());

        return ResponseEntity.ok(new Response(null, false, "Integration key deactivated successfully"));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<Response> activateApiKey(@PathVariable String id) throws EntityNotFoundException {
        IntegrationKey integrationKey = integrationKeyRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException("IntegrationKey", id));

        integrationKey.setActive(true);
        integrationKey.setUpdatedAt(LocalDateTime.now());
        integrationKeyRepository.save(integrationKey);

        log.info("Activated integration key: {} ({})", id, integrationKey.getName());

        return ResponseEntity.ok(new Response(null, false, "Integration key activated successfully"));
    }

    private String generateApiKey(String prefix) {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        String randomPart = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        return (prefix != null ? prefix + "_" : "key_") + randomPart;
    }

    private Map<String, Object> maskResponse(IntegrationKey integrationKey) {
        Map<String, Object> masked = new LinkedHashMap<>();
        masked.put("id", integrationKey.getId());
        masked.put("name", integrationKey.getName());
        masked.put("apiKeyMasked", maskString(integrationKey.getApiKey()));
        masked.put("authMode", integrationKey.getAuthMode());
        masked.put("webhookType", integrationKey.getWebhookType());
        masked.put("role", integrationKey.getRole());
        masked.put("partnerId", integrationKey.getPartnerId());
        masked.put("restaurantId", integrationKey.getRestaurantId());
        masked.put("allowedIps", integrationKey.getAllowedIps());
        masked.put("requireIpValidation", integrationKey.isRequireIpValidation());
        masked.put("active", integrationKey.isActive());
        masked.put("lastUsedAt", integrationKey.getLastUsedAt());
        masked.put("createdAt", integrationKey.getCreatedAt());
        masked.put("updatedAt", integrationKey.getUpdatedAt());
        return masked;
    }

    private String maskString(String value) {
        if (value == null || value.length() <= 12) {
            return "****";
        }
        return value.substring(0, 8) + "..." + value.substring(value.length() - 4);
    }

    // Request DTOs

    @Data
    public static class CreateIntegrationKeyRequest {
        @NotBlank(message = "Name is required")
        private String name;

        private String keyPrefix; // e.g., "pos_pk_live", "dlv_pk_live"
        private String secret;
        private Set<String> allowedIps;
        private boolean requireIpValidation;
        private AuthMode authMode;
        private String partnerId;
        private String restaurantId;
        private WebhookType webhookType;
        private String role;
    }

    @Data
    public static class UpdateIntegrationKeyRequest {
        private String name;
        private String secret;
        private Set<String> allowedIps;
        private Boolean requireIpValidation;
        private AuthMode authMode;
        private String partnerId;
        private String restaurantId;
        private WebhookType webhookType;
        private String role;
        private Boolean active;
    }
}
