package com.microel.microelhub.storage;

import com.microel.microelhub.common.chat.Platform;
import com.microel.microelhub.storage.entity.User;
import com.microel.microelhub.storage.repository.UserRepository;
import org.springframework.stereotype.Component;

@Component
public class UserDispatcher {
    private final UserRepository userRepository;

    public UserDispatcher(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User upsert(String userId, String phone, String name, Platform platform) {
        User found = userRepository.findTopByUserId(userId);
        if(found != null){
            found.setPhone(phone);
            found.setName(name);
            found.setPlatform(platform);
            return userRepository.save(found);
        }else{
            User created = new User(userId,phone,name, platform);
            return userRepository.save(created);
        }
    }
}
