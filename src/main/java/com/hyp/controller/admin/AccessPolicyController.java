package com.hyp.controller.admin;

import com.hyp.entity.AccessPolicy;
import com.hyp.exception.EntityNotFoundException;
import com.hyp.repository.AccessPolicyRepository;
import com.hyp.response.Response;
import com.hyp.security.policy.AccessPolicyService;
import jakarta.validation.Valid;
import java.util.Collections;
import java.util.List;
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
@RequestMapping("/admin/policies")
@RequiredArgsConstructor
public class AccessPolicyController {

    private final AccessPolicyRepository accessPolicyRepository;
    private final AccessPolicyService accessPolicyService;

    @GetMapping
    public ResponseEntity<Response> listPolicies(
            @RequestParam(required = false) String resource,
            @RequestParam(required = false) String operation,
            @RequestParam(required = false) Boolean active) {

        List<AccessPolicy> policies;

        if (resource != null && operation != null) {
            policies = accessPolicyRepository.findByResourceContainingAndOperation(resource, operation);
        } else if (resource != null) {
            policies = accessPolicyRepository.findByResourceContaining(resource);
        } else if (active != null) {
            policies = active
                    ? accessPolicyRepository.findByActiveTrue()
                    : accessPolicyRepository.findAll().stream()
                            .filter(p -> !p.isActive())
                            .toList();
        } else {
            policies = accessPolicyRepository.findAll();
        }

        return ResponseEntity.ok(new Response(policies, false, "Policies retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Response> getPolicy(@PathVariable String id) throws EntityNotFoundException {
        AccessPolicy policy =
                accessPolicyRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Policy", id));

        return ResponseEntity.ok(
                new Response(Collections.singletonList(policy), false, "Policy retrieved successfully"));
    }

    @PostMapping
    public ResponseEntity<Response> createPolicy(@RequestBody @Valid AccessPolicy policy) {
        policy.setId(null); // Ensure new ID is generated

        AccessPolicy savedPolicy = accessPolicyRepository.save(policy);
        log.info(
                "Created new policy: {} {} -> {}",
                savedPolicy.getResource(),
                savedPolicy.getOperation(),
                savedPolicy.getPermissions());

        // Refresh cache to include new policy
        accessPolicyService.refreshCache();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new Response(Collections.singletonList(savedPolicy), false, "Policy created successfully"));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Response> updatePolicy(@PathVariable String id, @RequestBody AccessPolicy policyUpdate)
            throws EntityNotFoundException {

        AccessPolicy existingPolicy =
                accessPolicyRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Policy", id));

        // Update only non-null fields
        if (policyUpdate.getResource() != null) {
            existingPolicy.setResource(policyUpdate.getResource());
        }
        if (policyUpdate.getOperation() != null) {
            existingPolicy.setOperation(policyUpdate.getOperation());
        }
        if (policyUpdate.getPermissions() != null) {
            existingPolicy.setPermissions(policyUpdate.getPermissions());
        }
        if (policyUpdate.getPriority() > 0) {
            existingPolicy.setPriority(policyUpdate.getPriority());
        }

        // Boolean fields need explicit handling
        existingPolicy.setPublic(policyUpdate.isPublic());
        existingPolicy.setRestaurantScoped(policyUpdate.isRestaurantScoped());
        existingPolicy.setOwnerOnly(policyUpdate.isOwnerOnly());
        existingPolicy.setWebhookAuth(policyUpdate.isWebhookAuth());
        existingPolicy.setActive(policyUpdate.isActive());

        AccessPolicy savedPolicy = accessPolicyRepository.save(existingPolicy);
        log.info("Updated policy: {}", savedPolicy.getId());

        // Refresh cache
        accessPolicyService.refreshCache();

        return ResponseEntity.ok(
                new Response(Collections.singletonList(savedPolicy), false, "Policy updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Response> deletePolicy(@PathVariable String id) throws EntityNotFoundException {
        if (!accessPolicyRepository.existsById(id)) {
            throw new EntityNotFoundException("Policy", id);
        }

        accessPolicyRepository.deleteById(id);
        log.info("Deleted policy: {}", id);

        // Refresh cache
        accessPolicyService.refreshCache();

        return ResponseEntity.ok(new Response(null, false, "Policy deleted successfully"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<Response> refreshCache() {
        accessPolicyService.refreshCache();
        log.info("Policy cache refreshed");

        return ResponseEntity.ok(new Response(null, false, "Policy cache refreshed successfully"));
    }
}
