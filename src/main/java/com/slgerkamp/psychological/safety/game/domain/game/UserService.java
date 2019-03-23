package com.slgerkamp.psychological.safety.game.domain.game;

import com.slgerkamp.psychological.safety.game.infra.model.User;
import com.slgerkamp.psychological.safety.game.infra.model.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class UserService {


    @Autowired
    private UserRepository userRepository;

    public UserStatus userStatus(String userid){
        UserStatus userStatus = UserStatus.NOT_EXIST;
        Optional<User> optUser = userRepository.findById(userid);
        if (optUser.isPresent()) {
            userStatus = UserStatus.valueOf(optUser.get().status);
        }
        return userStatus;
    }
}
