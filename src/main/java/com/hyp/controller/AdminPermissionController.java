package com.hyp.controller;

import com.hyp.entity.Permission;
import com.hyp.response.Response;
import com.hyp.security.service.PermissionService;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/admin/permissions")
@RequiredArgsConstructor
public class AdminPermissionController {

    private final PermissionService permissionService;

    @GetMapping
    public ResponseEntity<Response> getAllPermissions() {
        List<Permission> permissions = permissionService.getAllPermissions();
        return ResponseEntity.ok(new Response(permissions, false, "Permissions retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Response> getPermission(@PathVariable String id) {
        // Find by ID - need to iterate through cache or query repository
        List<Permission> allPermissions = permissionService.getAllPermissions();
        Permission permission = allPermissions.stream()
                .filter(p -> id.equals(p.getId()))
                .findFirst()
                .orElse(null);

        if (permission == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Response(null, true, "Permission not found"));
        }

        return ResponseEntity.ok(
                new Response(Collections.singletonList(permission), false, "Permission retrieved successfully"));
    }

    @PostMapping
    public ResponseEntity<Response> createPermission(@RequestBody PermissionRequest request) {
        try {
            Permission permission = Permission.builder()
                    .name(request.name())
                    .displayName(request.displayName())
                    .description(request.description())
                    .category(request.category())
                    .isSystem(false) // Custom permissions are never system permissions
                    .active(true)
                    .build();

            Permission created = permissionService.createPermission(permission);
            log.info("Created new permission: {}", created.getName());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new Response(Collections.singletonList(created), false, "Permission created successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new Response(null, true, e.getMessage()));
        }
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Response> updatePermission(@PathVariable String id, @RequestBody PermissionRequest request) {
        List<Permission> allPermissions = permissionService.getAllPermissions();
        Permission existing = allPermissions.stream()
                .filter(p -> id.equals(p.getId()))
                .findFirst()
                .orElse(null);

        if (existing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Response(null, true, "Permission not found"));
        }

        if (existing.isSystem()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new Response(null, true, "Cannot modify system permissions"));
        }

        // Update fields if provided
        if (request.displayName() != null) {
            existing.setDisplayName(request.displayName());
        }
        if (request.description() != null) {
            existing.setDescription(request.description());
        }
        if (request.category() != null) {
            existing.setCategory(request.category());
        }
        if (request.active() != null) {
            existing.setActive(request.active());
        }

        Permission updated = permissionService.updatePermission(existing);
        log.info("Updated permission: {}", updated.getName());

        return ResponseEntity.ok(
                new Response(Collections.singletonList(updated), false, "Permission updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Response> deletePermission(@PathVariable String id) {
        try {
            boolean deleted = permissionService.deletePermission(id);

            if (!deleted) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new Response(null, true, "Permission not found"));
            }

            log.info("Deleted permission with ID: {}", id);
            return ResponseEntity.ok(new Response(null, false, "Permission deleted successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new Response(null, true, e.getMessage()));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<Response> refreshCache() {
        permissionService.refreshCache();
        log.info("Permission cache refreshed");
        return ResponseEntity.ok(new Response(null, false, "Permission cache refreshed successfully"));
    }

    @GetMapping("/categories")
    public ResponseEntity<Response> getCategories() {
        List<Permission> permissions = permissionService.getAllPermissions();

        // Group permissions by category
        Map<String, List<Permission>> byCategory = permissions.stream()
                .collect(Collectors.groupingBy(p -> p.getCategory() != null ? p.getCategory() : "UNCATEGORIZED"));

        return ResponseEntity.ok(new Response(
                Collections.singletonList(byCategory), false, "Permission categories retrieved successfully"));
    }

    public record PermissionRequest(
            String name, String displayName, String description, String category, Boolean active) {}
}
