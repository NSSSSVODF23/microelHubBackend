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

    public User upsert(String userId, String name, Platform platform) {
        User found = userRepository.findTopByUserIdAndPlatform(userId, platform);
        if (found != null) {
            found.setName(name);
            return userRepository.save(found);
        } else {
            User created = new User(userId, null, null, name, platform);
            return userRepository.save(created);
        }
    }

    public User appendPhone(String userId, Platform platform, String phone) throws Exception {
        if(userId == null || userId.isBlank()) throw new Exception("Пустой идентификатор пользователя");
        if(platform == null) throw new Exception("Пустая платформа");
        if(phone == null || phone.isBlank()) throw new Exception("Пустой телефон");
        User found = userRepository.findTopByUserIdAndPlatform(userId, platform);
        if (found == null) throw new Exception("Пользователь " + userId + " не найден");
        found.getPhones().add(phone);
        return userRepository.save(found);
    }

    public User setLogin(String userId, Platform platform, String login) throws Exception {
        if(userId == null || userId.isBlank()) throw new Exception("Пустой идентификатор пользователя");
        if(platform == null) throw new Exception("Пустая платформа");
        if(login == null || login.isBlank()) throw new Exception("Пустой логин");
        User found = userRepository.findTopByUserIdAndPlatform(userId, platform);
        if (found == null) throw new Exception("Пользователь " + userId + " не найден");
        found.setBillingLogin(login);
        return userRepository.save(found);
    }
}
