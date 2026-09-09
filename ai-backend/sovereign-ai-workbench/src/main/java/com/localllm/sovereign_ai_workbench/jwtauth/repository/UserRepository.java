package com.localllm.sovereign_ai_workbench.jwtauth.repository;

import com.localllm.sovereign_ai_workbench.jwtauth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Boolean existsByUsername(String username);

    Boolean existsByEmail(String email);
}
