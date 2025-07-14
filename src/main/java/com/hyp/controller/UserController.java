package com.hyp.controller;

import java.util.Collections;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hyp.dto.UserDto;
import com.hyp.entity.User;
import com.hyp.exception.EntityNotFoundException;
import com.hyp.request.UserLoginRequest;
import com.hyp.response.Response;
import com.hyp.service.UserService;
import com.hyp.translation.UserTranslation;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/user")
public class UserController extends BaseListController<UserDto, User, String> {

	@Autowired
	public UserTranslation userTranslation;
	
	@Autowired
	public UserService userService;
	
	@Autowired
	private PasswordEncoder passwordEncoder;
	
	@PostMapping("/auth/login")
	public ResponseEntity<Response> login(@RequestBody @Valid UserLoginRequest userLoginRequest) throws EntityNotFoundException {
	    User user = Optional.ofNullable(userService.findByEmail(userLoginRequest.getEmail()))
		.orElseThrow(() -> new EntityNotFoundException("User", userLoginRequest.getEmail()));
	    
	    if(!passwordEncoder.matches(userLoginRequest.getPassword(), user.getPassword())) {
	        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new Response(null, true, "Invalid credentials"));
	    }
	    
	    UserDto userData = userTranslation.getDto(user);
	    return ResponseEntity.ok(new Response(Collections.singletonList(userData), false, "Login successful"));
	}

	@PostMapping
	public ResponseEntity<Response> create(@RequestBody @Valid UserDto userDto) {
		boolean emailExists = userService.findByEmail(userDto.getEmail()) != null;
		boolean restaurantExists = userService.findByRestaurantId(userDto.getRestaurantId()) != null;

		if (emailExists) {
			return ResponseEntity.status(HttpStatus.CONFLICT)
					.body(new Response(null, false, "Email is already registered with another user."));
		}

		if (restaurantExists) {
			return ResponseEntity.status(HttpStatus.CONFLICT)
					.body(new Response(null, false, "A user already exists for this restaurant."));
		}

		User user = userTranslation.getEntity(userDto);
		User savedUser = userService.create(user);

		return ResponseEntity.status(HttpStatus.CREATED)
				.body(new Response(Collections.singletonList(savedUser), true, "User created successfully."));
	}
}
