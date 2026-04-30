package com.hyp.controller.admin;

import com.hyp.entity.Role;
import com.hyp.exception.BadRequestException;
import com.hyp.exception.EntityNotFoundException;
import com.hyp.repository.RoleRepository;
import com.hyp.response.Response;
import com.hyp.security.service.RoleService;
import jakarta.validation.Valid;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
@RequestMapping("/admin/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleRepository roleRepository;
    private final RoleService roleService;

    @GetMapping
    public ResponseEntity<Response> listRoles(@RequestParam(required = false) Boolean active) {
        List<Role> roles;

        if (active != null) {
            roles = active
                    ? roleRepository.findByActiveTrue()
                    : roleRepository.findAll().stream()
                            .filter(r -> !r.isActive())
                            .toList();
        } else {
            roles = roleRepository.findAll();
        }

        return ResponseEntity.ok(new Response(roles, false, "Roles retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Response> getRole(@PathVariable String id) throws EntityNotFoundException {
        Role role = roleRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Role", id));

        return ResponseEntity.ok(new Response(Collections.singletonList(role), false, "Role retrieved successfully"));
    }

    @GetMapping("/{name}/effective")
    public ResponseEntity<Response> getEffectiveRoles(@PathVariable String name) throws EntityNotFoundException {
        Role role = roleRepository.findByName(name).orElseThrow(() -> new EntityNotFoundException("Role", name));

        Set<String> effectiveRoles = roleService.getEffectiveRoles(name);
        Set<String> effectivePermissions = roleService.getEffectivePermissions(name);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("role", role);
        result.put("effectiveRoles", effectiveRoles);
        result.put("totalRoles", effectiveRoles.size());
        result.put("effectivePermissions", effectivePermissions);
        result.put("totalPermissions", effectivePermissions.size());

        return ResponseEntity.ok(new Response(
                Collections.singletonList(result), false, "Effective roles and permissions retrieved successfully"));
    }

    @PostMapping
    public ResponseEntity<Response> createRole(@RequestBody @Valid Role role) throws BadRequestException {
        // Check if role name already exists
        if (roleRepository.findByName(role.getName()).isPresent()) {
            throw new BadRequestException("Role", "Role with name '" + role.getName() + "' already exists");
        }

        role.setId(null); // Ensure new ID is generated
        role.setSystem(false); // User-created roles are not system roles

        Role savedRole = roleRepository.save(role);
        log.info("Created new role: {}", savedRole.getName());

        // Refresh cache to include new role
        roleService.refreshCache();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new Response(Collections.singletonList(savedRole), false, "Role created successfully"));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Response> updateRole(@PathVariable String id, @RequestBody Role roleUpdate)
            throws EntityNotFoundException, BadRequestException {

        Role existingRole = roleRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Role", id));

        // Prevent modification of system role's name and isSystem flag
        if (existingRole.isSystem()) {
            if (roleUpdate.getName() != null && !roleUpdate.getName().equals(existingRole.getName())) {
                throw new BadRequestException("Role", "Cannot change name of system role");
            }
        }

        // Update only non-null fields
        if (roleUpdate.getDisplayName() != null) {
            existingRole.setDisplayName(roleUpdate.getDisplayName());
        }
        if (roleUpdate.getDescription() != null) {
            existingRole.setDescription(roleUpdate.getDescription());
        }
        if (roleUpdate.getPriority() > 0) {
            existingRole.setPriority(roleUpdate.getPriority());
        }
        if (roleUpdate.getInheritsFrom() != null) {
            existingRole.setInheritsFrom(roleUpdate.getInheritsFrom());
        }
        if (roleUpdate.getPermissions() != null) {
            existingRole.setPermissions(roleUpdate.getPermissions());
        }

        // Boolean fields - only update if role is not a system role or is explicitly setting active
        if (!existingRole.isSystem()) {
            existingRole.setActive(roleUpdate.isActive());
        }

        // Allow updating isSuperAdmin only for non-system roles
        if (!existingRole.isSystem()) {
            existingRole.setSuperAdmin(roleUpdate.isSuperAdmin());
        }

        Role savedRole = roleRepository.save(existingRole);
        log.info("Updated role: {}", savedRole.getName());

        // Refresh cache
        roleService.refreshCache();

        return ResponseEntity.ok(
                new Response(Collections.singletonList(savedRole), false, "Role updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Response> deleteRole(@PathVariable String id)
            throws EntityNotFoundException, BadRequestException {
        Role role = roleRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Role", id));

        // Prevent deletion of system roles
        if (role.isSystem()) {
            throw new BadRequestException("Role", "Cannot delete system role: " + role.getName());
        }

        roleRepository.deleteById(id);
        log.info("Deleted role: {}", role.getName());

        // Refresh cache
        roleService.refreshCache();

        return ResponseEntity.ok(new Response(null, false, "Role deleted successfully"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<Response> refreshCache() {
        roleService.refreshCache();
        log.info("Role cache refreshed");

        return ResponseEntity.ok(new Response(null, false, "Role cache refreshed successfully"));
    }
}
