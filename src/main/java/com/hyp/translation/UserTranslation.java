package com.hyp.translation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.hyp.dto.UserDto;
import com.hyp.entity.User;
import com.hyp.service.BaseTranslationServiceImpl;

@Service
public class UserTranslation extends BaseTranslationServiceImpl<UserDto, User> {

	@Autowired
    private PasswordEncoder passwordEncoder;
	
	@Override
	protected Class<UserDto> getDtoClass() {
		return UserDto.class;
	}

	@Override
	protected Class<User> getEntityClass() {
		return User.class;
	}
	
	@Override
    public User getEntity(UserDto dto) {
        User user = super.getEntity(dto);
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        return user;
    }

}
