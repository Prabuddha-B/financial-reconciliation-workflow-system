package com.frwss.system.repository;

import com.frwss.system.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository
        extends JpaRepository<User,String>{
}