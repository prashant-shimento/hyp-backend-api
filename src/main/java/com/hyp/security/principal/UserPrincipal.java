package com.hyp.security.principal;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPrincipal implements UserDetails {

    private String userId;
    private String userType; // CUSTOMER, USER, PARTNER
    private String role;
    private String email;
    private String mobile;
    private String name;
    private String restaurantId; // For restaurant-scoped users
    private Set<String> restaurants; // For customers (multi-restaurant)
    private String partnerId; // For partner users
    private boolean isSuperAdmin;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() {
        return null; // Not used for JWT auth
    }

    @Override
    public String getUsername() {
        return userId;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public boolean canAccessRestaurant(String targetRestaurantId) {
        if (isSuperAdmin) {
            return true;
        }
        if (restaurantId != null && restaurantId.equals(targetRestaurantId)) {
            return true;
        }
        if (restaurants != null && restaurants.contains(targetRestaurantId)) {
            return true;
        }
        return false;
    }
}
