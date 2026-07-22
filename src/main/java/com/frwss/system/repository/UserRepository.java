package com.frwss.system.repository;

import com.frwss.system.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByUsername(String username);

    @Query("SELECT COALESCE(MAX(u.userId),0) FROM User u")
    Integer findMaxUserId();
}
