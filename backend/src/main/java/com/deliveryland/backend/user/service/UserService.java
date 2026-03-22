package com.deliveryland.backend.user.service;

import com.deliveryland.backend.user.UserRepository;
import com.deliveryland.backend.user.dto.UserResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public UserResponse getCurrentUser() {
        return null;
    }

//    @Transactional
//    public UserResponse updateCurrentUser(UserDto userDto) {
//        return null;
//    }

    @Transactional
    public void deleteUser() {
    }

}
