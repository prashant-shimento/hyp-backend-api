package com.hyp.service;

import com.hyp.client.OneSignalClient;
import com.hyp.entity.User;
import com.hyp.exception.OneSignalException;
import com.hyp.repository.UserRepository;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService extends BaseServiceImpl<User, String> {

    private static final String CACHE_NAME = "users";

    @Autowired
    public UserRepository userRepository;

    @Autowired
    public OneSignalClient oneSignalClient;

    @Override
    protected String cacheName() {
        return CACHE_NAME;
    }

    @Override
    protected Class<User> entityType() {
        return User.class;
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public List<User> findByRestaurantId(String restaurantId) {
        return userRepository.findByRestaurantIdsContaining(restaurantId);
    }

    public boolean existsByRestaurantId(String restaurantId) {
        return userRepository.existsByRestaurantIdsContaining(restaurantId);
    }

    public void registerOneSignalUser(String userId, String oneSignalId) throws OneSignalException {
        oneSignalClient.registerUser(userId, oneSignalId).block();
    }

    public void unregisterOneSignalUser(String userId, String oneSignalId) {
        oneSignalClient.deleteUser(userId, oneSignalId).block();
    }

    public User create(@Valid User user) {
        return userRepository.save(user);
    }
}
