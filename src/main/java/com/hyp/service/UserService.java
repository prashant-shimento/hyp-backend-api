package com.hyp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hyp.client.OneSignalClient;
import com.hyp.entity.User;
import com.hyp.exception.OneSignalException;
import com.hyp.repository.UserRepository;

@Service
public class UserService extends BaseServiceImpl<User, String> {

	@Autowired
	public UserRepository userRepository;
	
	@Autowired
	public OneSignalClient oneSignalClient;

	public User findByEmail(String email) {
		return userRepository.findByEmail(email);
	}
	
	public User findByRestaurantId(String restaurantId) {
		return userRepository.findByRestaurantId(restaurantId);
	}
	
	public void registerOneSignalUser(String userId, String oneSignalId) throws OneSignalException {
		oneSignalClient.registerUser(userId, oneSignalId).block();
	}
}
