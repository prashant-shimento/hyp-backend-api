package com.hyp.controller;

import com.hyp.dto.UserDto;
import com.hyp.entity.User;
import com.hyp.exception.EntityNotFoundException;
import com.hyp.request.UserLoginRequest;
import com.hyp.response.Response;
import com.hyp.security.jwt.JwtTokenService;
import com.hyp.security.principal.UserPrincipal;
import com.hyp.service.UserService;
import com.hyp.translation.UserTranslation;
import jakarta.validation.Valid;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = {"/api/v2/user", "/api/v3/user"})
public class UserController extends BaseListController<UserDto, User, String> {

    @Autowired
    public UserTranslation userTranslation;

    @Autowired
    public UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenService jwtTokenService;

    @PostMapping("/auth/login")
    public ResponseEntity<Response> login(@RequestBody @Valid UserLoginRequest userLoginRequest)
            throws EntityNotFoundException {
        User user = Optional.ofNullable(userService.findByEmail(userLoginRequest.getEmail()))
                .orElseThrow(() -> new EntityNotFoundException("User", userLoginRequest.getEmail()));

        if (!passwordEncoder.matches(userLoginRequest.getPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new Response(null, true, "Invalid credentials"));
        }

        // Determine role - default to RESTAURANT_USER if not set
        String role = user.getRole() != null ? user.getRole() : "RESTAURANT_USER";

        // Get first restaurant as the default/active restaurant for the session
        String defaultRestaurantId =
                user.getRestaurantIds() != null && !user.getRestaurantIds().isEmpty()
                        ? user.getRestaurantIds().iterator().next()
                        : null;

        // Generate JWT tokens for authenticated user
        UserPrincipal principal = UserPrincipal.builder()
                .userId(user.getId())
                .userType("USER")
                .role(role)
                .email(user.getEmail())
                .name(user.getName())
                .mobile(user.getMobile())
                .restaurantId(defaultRestaurantId)
                .partnerId(user.getPartnerId())
                .build();

        Map<String, Object> tokens = jwtTokenService.createTokenPair(principal, null, null);

        UserDto userData = userTranslation.getDto(user);

        // Build response with user data and tokens
        Map<String, Object> responseData = new LinkedHashMap<>();
        responseData.put("user", userData);
        responseData.put("accessToken", tokens.get("accessToken"));
        responseData.put("refreshToken", tokens.get("refreshToken"));
        responseData.put("tokenType", tokens.get("tokenType"));
        responseData.put("expiresIn", tokens.get("expiresIn"));

        return ResponseEntity.ok(new Response(Collections.singletonList(responseData), false, "Login successful"));
    }

    @PostMapping
    public ResponseEntity<Response> create(@RequestBody @Valid UserDto userDto) {
        boolean emailExists = userService.findByEmail(userDto.getEmail()) != null;

        if (emailExists) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new Response(null, false, "Email is already registered with another user."));
        }

        User user = userTranslation.getEntity(userDto);
        User savedUser = userService.create(user);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new Response(Collections.singletonList(savedUser), true, "User created successfully."));
    }
}
