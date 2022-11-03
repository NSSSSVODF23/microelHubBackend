package com.microel.microelhub.storage.repository;

import com.microel.microelhub.storage.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UserRepository extends JpaRepository<User, String>, JpaSpecificationExecutor<User> {
    User findTopByUserId(String userId);
}
