package com.hyp.security.service;

import com.hyp.entity.Role;
import com.hyp.repository.RoleRepository;
import com.hyp.security.service.SecurityCacheService.CacheInvalidationType;
import jakarta.annotation.PostConstruct;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
@Lazy(false)
@RequiredArgsConstructor
@Slf4j
public class RoleService {

    private final RoleRepository roleRepository;
    private final SecurityCacheService securityCacheService;

    @PostConstruct
    public void init() {
        refreshCache();
    }

    /**
     * Refresh the role cache from database and store in L1/L2 cache
     */
    public void refreshCache() {
        log.info("Refreshing role cache...");

        List<Role> roles = roleRepository.findByActiveTrue();
        Map<String, Role> roleCache = new HashMap<>();
        roles.forEach(role -> roleCache.put(role.getName(), role));

        securityCacheService.cacheRoles(roleCache);

        log.info("Role cache refreshed. Loaded {} roles", roleCache.size());
    }

    /**
     * Refresh cache and notify other instances
     */
    public void refreshCacheAndNotify() {
        refreshCache();
        securityCacheService.publishCacheInvalidation(CacheInvalidationType.ROLES);
    }

    /**
     * Get a role by name from cache
     */
    public Optional<Role> getRole(String roleName) {
        Map<String, Role> roleCache = securityCacheService.getCachedRoles(Role.class);
        return Optional.ofNullable(roleCache.get(roleName));
    }

    /**
     * Check if a role is a super admin
     */
    public boolean isSuperAdmin(String roleName) {
        return getRole(roleName).map(Role::isSuperAdmin).orElse(false);
    }

    /**
     * Get all effective roles for a given role (including inherited roles)
     */
    public Set<String> getEffectiveRoles(String roleName) {
        Map<String, Role> roleCache = securityCacheService.getCachedRoles(Role.class);
        return computeEffectiveRoles(roleName, roleCache);
    }

    /**
     * Compute effective roles recursively (including inherited roles)
     */
    private Set<String> computeEffectiveRoles(String roleName, Map<String, Role> roleCache) {
        Set<String> effectiveRoles = new HashSet<>();
        computeEffectiveRolesRecursive(roleName, effectiveRoles, new HashSet<>(), roleCache);
        return Collections.unmodifiableSet(effectiveRoles);
    }

    private void computeEffectiveRolesRecursive(
            String roleName, Set<String> effectiveRoles, Set<String> visited, Map<String, Role> roleCache) {
        // Prevent infinite loops
        if (visited.contains(roleName)) {
            return;
        }
        visited.add(roleName);

        // Add current role
        effectiveRoles.add(roleName);

        // Get role from cache
        Role role = roleCache.get(roleName);
        if (role == null || role.getInheritsFrom() == null) {
            return;
        }

        // Add inherited roles recursively
        for (String parentRole : role.getInheritsFrom()) {
            computeEffectiveRolesRecursive(parentRole, effectiveRoles, visited, roleCache);
        }
    }

    /**
     * Check if user has a specific role (including through inheritance)
     */
    public boolean hasRole(String userRole, String requiredRole) {
        Set<String> effectiveRoles = getEffectiveRoles(userRole);
        return effectiveRoles.contains(requiredRole);
    }

    /**
     * Check if user has any of the required roles (including through inheritance)
     */
    public boolean hasAnyRole(String userRole, List<String> requiredRoles) {
        if (requiredRoles == null || requiredRoles.isEmpty()) {
            return true; // No roles required
        }
        Set<String> effectiveRoles = getEffectiveRoles(userRole);
        return requiredRoles.stream().anyMatch(effectiveRoles::contains);
    }

    /**
     * Get all effective permissions for a given role (including inherited roles' permissions)
     */
    public Set<String> getEffectivePermissions(String roleName) {
        Map<String, Role> roleCache = securityCacheService.getCachedRoles(Role.class);
        return computeEffectivePermissions(roleName, roleCache);
    }

    /**
     * Compute effective permissions for a role (including inherited roles' permissions)
     */
    private Set<String> computeEffectivePermissions(String roleName, Map<String, Role> roleCache) {
        Set<String> effectivePermissions = new HashSet<>();

        // Get all effective roles (including inherited) and collect their permissions
        Set<String> effectiveRoles = computeEffectiveRoles(roleName, roleCache);
        for (String role : effectiveRoles) {
            Role roleObj = roleCache.get(role);
            if (roleObj != null && roleObj.getPermissions() != null) {
                effectivePermissions.addAll(roleObj.getPermissions());
            }
        }

        return Collections.unmodifiableSet(effectivePermissions);
    }

    /**
     * Check if user has a specific permission (including through role inheritance)
     */
    public boolean hasPermission(String userRole, String requiredPermission) {
        Set<String> effectivePermissions = getEffectivePermissions(userRole);
        return effectivePermissions.contains(requiredPermission);
    }

    /**
     * Check if user has any of the required permissions (including through role inheritance)
     */
    public boolean hasAnyPermission(String userRole, List<String> requiredPermissions) {
        if (requiredPermissions == null || requiredPermissions.isEmpty()) {
            return true; // No permissions required
        }
        Set<String> effectivePermissions = getEffectivePermissions(userRole);
        return requiredPermissions.stream().anyMatch(effectivePermissions::contains);
    }

    /**
     * Get all roles from cache
     */
    public List<Role> getAllRoles() {
        Map<String, Role> roleCache = securityCacheService.getCachedRoles(Role.class);
        return List.copyOf(roleCache.values());
    }

    /**
     * Create a new role
     */
    public Role createRole(Role role) {
        Role saved = roleRepository.save(role);
        refreshCacheAndNotify();
        return saved;
    }

    /**
     * Update an existing role
     */
    public Role updateRole(Role role) {
        Role saved = roleRepository.save(role);
        refreshCacheAndNotify();
        return saved;
    }

    /**
     * Delete a role (only non-system roles)
     */
    public boolean deleteRole(String roleId) {
        Optional<Role> roleOpt = roleRepository.findById(roleId);
        if (roleOpt.isEmpty()) {
            return false;
        }

        Role role = roleOpt.get();
        if (role.isSystem()) {
            throw new IllegalArgumentException("Cannot delete system roles");
        }

        roleRepository.deleteById(roleId);
        refreshCacheAndNotify();
        return true;
    }
}
