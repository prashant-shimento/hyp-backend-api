package com.hyp.security.service;

import com.hyp.entity.Permission;
import com.hyp.repository.PermissionRepository;
import com.hyp.security.service.SecurityCacheService.CacheInvalidationType;
import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
@Lazy(false)
@RequiredArgsConstructor
@Slf4j
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final SecurityCacheService securityCacheService;

    @PostConstruct
    public void init() {
        refreshCache();
    }

    /**
     * Refresh the permission cache from database and store in Redis
     */
    public void refreshCache() {
        log.info("Refreshing permission cache...");

        List<Permission> permissions = permissionRepository.findByActiveTrue();
        Map<String, Permission> permissionCache = new HashMap<>();
        permissions.forEach(permission -> permissionCache.put(permission.getName(), permission));

        // Store in Redis
        securityCacheService.cachePermissions(permissionCache);

        log.info("Permission cache refreshed. Loaded {} permissions", permissionCache.size());
    }

    /**
     * Refresh cache and notify other instances
     */
    public void refreshCacheAndNotify() {
        refreshCache();
        securityCacheService.publishCacheInvalidation(CacheInvalidationType.PERMISSIONS);
    }

    /**
     * Get a permission by name from cache
     */
    public Optional<Permission> getPermission(String name) {
        Map<String, Permission> permissionCache = securityCacheService.getCachedPermissions(Permission.class);
        return Optional.ofNullable(permissionCache.get(name));
    }

    /**
     * Check if a permission exists
     */
    public boolean permissionExists(String name) {
        Map<String, Permission> permissionCache = securityCacheService.getCachedPermissions(Permission.class);
        return permissionCache.containsKey(name);
    }

    /**
     * Get all permission names
     */
    public Set<String> getAllPermissionNames() {
        Map<String, Permission> permissionCache = securityCacheService.getCachedPermissions(Permission.class);
        return Set.copyOf(permissionCache.keySet());
    }

    /**
     * Get all permissions from cache
     */
    public List<Permission> getAllPermissions() {
        Map<String, Permission> permissionCache = securityCacheService.getCachedPermissions(Permission.class);
        return List.copyOf(permissionCache.values());
    }

    /**
     * Get permissions by category
     */
    public List<Permission> getPermissionsByCategory(String category) {
        Map<String, Permission> permissionCache = securityCacheService.getCachedPermissions(Permission.class);
        return permissionCache.values().stream()
                .filter(p -> category.equals(p.getCategory()))
                .collect(Collectors.toList());
    }

    /**
     * Validate that all permission names exist
     */
    public boolean validatePermissions(Set<String> permissionNames) {
        if (permissionNames == null || permissionNames.isEmpty()) {
            return true;
        }
        Map<String, Permission> permissionCache = securityCacheService.getCachedPermissions(Permission.class);
        return permissionNames.stream().allMatch(permissionCache::containsKey);
    }

    /**
     * Get invalid permissions from a set (returns names that don't exist)
     */
    public Set<String> getInvalidPermissions(Set<String> permissionNames) {
        if (permissionNames == null) {
            return Set.of();
        }
        Map<String, Permission> permissionCache = securityCacheService.getCachedPermissions(Permission.class);
        return permissionNames.stream()
                .filter(name -> !permissionCache.containsKey(name))
                .collect(Collectors.toSet());
    }

    /**
     * Create a new permission
     */
    public Permission createPermission(Permission permission) {
        Map<String, Permission> permissionCache = securityCacheService.getCachedPermissions(Permission.class);
        if (permissionCache.containsKey(permission.getName())) {
            throw new IllegalArgumentException("Permission already exists: " + permission.getName());
        }
        Permission saved = permissionRepository.save(permission);
        refreshCacheAndNotify();
        return saved;
    }

    /**
     * Update an existing permission
     */
    public Permission updatePermission(Permission permission) {
        Permission saved = permissionRepository.save(permission);
        refreshCacheAndNotify();
        return saved;
    }

    /**
     * Delete a permission (only non-system permissions)
     */
    public boolean deletePermission(String permissionId) {
        Optional<Permission> permissionOpt = permissionRepository.findById(permissionId);
        if (permissionOpt.isEmpty()) {
            return false;
        }

        Permission permission = permissionOpt.get();
        if (permission.isSystem()) {
            throw new IllegalArgumentException("Cannot delete system permissions");
        }

        permissionRepository.deleteById(permissionId);
        refreshCacheAndNotify();
        return true;
    }
}
